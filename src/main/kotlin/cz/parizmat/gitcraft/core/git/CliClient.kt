package cz.parizmat.gitcraft.core.git

import arrow.core.Either
import arrow.core.flatMap
import cz.parizmat.gitcraft.core.domain.base.GitError
import cz.parizmat.gitcraft.core.domain.base.ProcessError
import cz.parizmat.gitcraft.core.domain.element.Branch
import cz.parizmat.gitcraft.core.domain.element.Commit
import cz.parizmat.gitcraft.core.domain.element.GitChange
import cz.parizmat.gitcraft.core.domain.element.Repository
import cz.parizmat.gitcraft.core.domain.element.FileDiff
import cz.parizmat.gitcraft.core.domain.element.DiffRow
import cz.parizmat.gitcraft.core.domain.element.DiffRowType
import cz.parizmat.gitcraft.core.domain.element.enums.FileStatus
import cz.parizmat.gitcraft.core.domain.command.CommandResult
import arrow.core.left
import arrow.core.right
import java.nio.file.Path

class CliClient(
    private val commandExecutor: CommandExecutor,
) : GitClient {
    override suspend fun openRepository(path: Path): Either<ProcessError, Repository> {
        return commandExecutor.execute(
            workingDirPath = path,
            args = listOf(
                "rev-parse",
                "--show-toplevel",
            ),
        ).flatMap { rootResult ->

            commandExecutor.execute(
                workingDirPath = path,
                args = listOf(
                    "branch",
                    "--show-current",
                ),
            ).map { branchResult ->

                Repository(
                    rootPath = Path.of(
                        rootResult.stdout.removeSuffix("\n").removeSuffix("\r")
                    ),
                    currentBranch = branchResult.stdout
                        .trim()
                        .takeIf { it.isNotBlank() },
                )
            }

        }
    }

    override suspend fun status(repository: Repository): Either<GitError, List<GitChange>> {
        return run(repository, listOf("status", "--porcelain=v1", "-z", "--untracked-files=all")).map { result ->
            val entries = result.stdout.split('\u0000')
            val changes = mutableListOf<GitChange>()
            var i = 0
            while (i < entries.size) {
                val entry = entries[i++]
                if (entry.length < 4) continue
                val x = entry[0]
                val y = entry[1]
                val old = if (x in "RC" || y in "RC") entries.getOrNull(i++)?.let(Path::of) else null
                val conflict = entry.take(2) in setOf("DD", "AU", "UD", "UA", "DU", "AA", "UU")
                changes.add(GitChange(Path.of(entry.substring(3)), old,
                    if (conflict) FileStatus.CONFLICTED else fileStatus(x),
                    if (conflict) FileStatus.CONFLICTED else fileStatus(y)))
            }
            changes
        }
    }

    override suspend fun hasCommits(repository: Repository): Either<GitError, Boolean> =
        run(repository, listOf("rev-parse", "--verify", "--quiet", "HEAD"), setOf(0, 1)).map { it.exitCode == 0 }

    override suspend fun stageFile(repository: Repository, change: GitChange): Either<GitError, Unit> =
        mutate(repository, listOf("add", "-A", "--") + if (change.workingTreeStatus == FileStatus.RENAMED) paths(change) else listOf(change.path.toString()))

    override suspend fun stageAll(repository: Repository): Either<GitError, Unit> =
        mutate(repository, listOf("add", "-A", "--", "."))

    override suspend fun unstageFile(repository: Repository, change: GitChange): Either<GitError, Unit> =
        hasCommits(repository).flatMap { exists ->
            mutate(repository, (if (exists) listOf("reset", "-q", "HEAD", "--")
            else listOf("rm", "--cached", "-r", "-f", "--ignore-unmatch", "--")) + paths(change))
        }

    override suspend fun unstageAll(repository: Repository): Either<GitError, Unit> =
        hasCommits(repository).flatMap { exists ->
            mutate(repository, if (exists) listOf("reset", "-q", "HEAD", "--", ".")
            else listOf("rm", "--cached", "-r", "-f", "--ignore-unmatch", "--", "."))
        }

    override suspend fun commit(repository: Repository, message: String, amend: Boolean): Either<GitError, Unit> {
        if (message.isBlank()) return failure("Commit message cannot be empty.")
        val changes = when (val result = status(repository)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        if (changes.any { it.indexStatus == FileStatus.CONFLICTED || it.workingTreeStatus == FileStatus.CONFLICTED })
            return failure("Resolve merge conflicts before committing.")
        if (changes.none { it.indexStatus !in setOf(FileStatus.UNMODIFIED, FileStatus.UNTRACKED, FileStatus.IGNORED) })
            return failure("Stage at least one change before committing.")
        return mutate(repository, listOf("commit") + (if (amend) listOf("--amend") else emptyList()) + listOf("-m", message))
    }

    override suspend fun discardChanges(repository: Repository): Either<GitError, Unit> {
        val changes = when (val result = status(repository)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        if (changes.any { it.indexStatus == FileStatus.CONFLICTED || it.workingTreeStatus == FileStatus.CONFLICTED })
            return failure("Resolve merge conflicts before discarding all changes.")
        val index = when (val result = run(repository, listOf("ls-files", "--stage", "-z"))) {
            is Either.Left -> return result
            is Either.Right -> result.value.stdout
        }
        if (index.split('\u0000').any { it.startsWith("160000 ") } || changes.any { java.nio.file.Files.exists(repository.rootPath.resolve(it.path).resolve(".git")) } || java.nio.file.Files.exists(repository.rootPath.resolve(".gitmodules")))
            return failure("Discard all is unavailable for repositories containing submodules or nested repositories.")
        val exists = when (val result = hasCommits(repository)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }
        // Capture untracked names before restoring the index. Never clean ignored files.
        val untracked = changes.filter { it.indexStatus == FileStatus.UNTRACKED }.map { it.path.toString() }
        if (!exists) {
            // With no HEAD there is no committed snapshot to restore safely.
            return failure("The repository has no commit to restore. Unstage files or create the first commit instead.")
        }
        val restore = mutate(repository, listOf("restore", "--source=HEAD", "--staged", "--worktree", "--", "."))
        if (restore is Either.Left) return restore
        for (path in untracked) {
            val clean = mutate(repository, listOf("clean", "-f", "--", path))
            if (clean is Either.Left) return clean
        }
        return Unit.right()
    }

    override suspend fun diff(repository: Repository, change: GitChange, staged: Boolean): Either<GitError, FileDiff> {
        if (!staged && change.workingTreeStatus == FileStatus.UNTRACKED) {
            val path = repository.rootPath.resolve(change.path).normalize()
            if (!path.startsWith(repository.rootPath.normalize())) return failure("File is outside the repository.")
            return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    if (java.nio.file.Files.isSymbolicLink(path)) {
                        FileDiff(change.path, listOf(DiffRow(DiffRowType.ADDED, java.nio.file.Files.readSymbolicLink(path).toString(), newLine = 1))).right()
                    } else if (java.nio.file.Files.size(path) > MAX_DIFF_BYTES) {
                        FileDiff(change.path, emptyList(), isTruncated = true).right()
                    } else {
                        val bytes = java.nio.file.Files.readAllBytes(path)
                        if (bytes.any { it == 0.toByte() }) FileDiff(change.path, emptyList(), isBinary = true).right()
                        else {
                            val lines = bytes.toString(Charsets.UTF_8).lineSequence().toList().let { if (it.lastOrNull() == "") it.dropLast(1) else it }
                            FileDiff(change.path, lines.take(MAX_DIFF_ROWS).mapIndexed { i, text -> DiffRow(DiffRowType.ADDED, text, newLine = i + 1) }, isTruncated = lines.size > MAX_DIFF_ROWS).right()
                        }
                    }
                } catch (error: java.io.IOException) { GitError.Unexpected(error).left() }
            }
        }
        return run(repository, listOf("diff", "--no-ext-diff", "--no-textconv", "--no-color", "--unified=3") +
            (if (staged) listOf("--cached") else emptyList()) + listOf("--") + paths(change)).map { result ->
            parseDiff(change.path, result.stdout)
        }
    }

    private fun parseDiff(path: Path, text: String): FileDiff {
        if (text.lineSequence().any { it == "GIT binary patch" || (it.startsWith("Binary files ") && it.endsWith(" differ")) })
            return FileDiff(path, emptyList(), isBinary = true)
        var old = 0
        var new = 0
        var inHunk = false
        val hunk = Regex("^@@ -(\\d+)(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@.*")
        val rows = text.take(MAX_DIFF_BYTES.toInt()).lineSequence().take(MAX_DIFF_ROWS).map { line ->
            val match = hunk.matchEntire(line)
            when {
                match != null -> {
                    old = match.groupValues[1].toInt(); new = match.groupValues[2].toInt(); inHunk = true
                    DiffRow(DiffRowType.HEADER, line)
                }
                inHunk && line.startsWith('+') -> DiffRow(DiffRowType.ADDED, line.drop(1), newLine = new++)
                inHunk && line.startsWith('-') -> DiffRow(DiffRowType.DELETED, line.drop(1), oldLine = old++)
                inHunk && line.startsWith(' ') -> DiffRow(DiffRowType.CONTEXT, line.drop(1), old++, new++)
                else -> DiffRow(DiffRowType.HEADER, line)
            }
        }.toList()
        return FileDiff(path, rows, isTruncated = text.length > MAX_DIFF_BYTES || text.count { it == '\n' } >= MAX_DIFF_ROWS)
    }

    private fun paths(change: GitChange): List<String> = listOfNotNull(change.path, change.oldPath).map(Path::toString).distinct()

    private fun fileStatus(value: Char): FileStatus = when (value) {
        'M', 'T' -> FileStatus.MODIFIED
        'A' -> FileStatus.ADDED
        'D' -> FileStatus.DELETED
        'R' -> FileStatus.RENAMED
        'C' -> FileStatus.COPIED
        '?' -> FileStatus.UNTRACKED
        '!' -> FileStatus.IGNORED
        'U' -> FileStatus.CONFLICTED
        else -> FileStatus.UNMODIFIED
    }

    private suspend fun run(repository: Repository, args: List<String>, accepted: Set<Int> = setOf(0)): Either<GitError, CommandResult> =
        commandExecutor.execute(repository.rootPath, args, accepted).mapLeft { error ->
            when (error) {
                is ProcessError.CommandFailed -> GitError.CommandFailed(error.command.joinToString(" "), error.exitCode, error.stderr)
                is ProcessError.ProcessInterrupted -> GitError.Unexpected(error.cause)
                is ProcessError.ProcessStartFailed -> GitError.Unexpected(error.cause)
            }
        }

    private suspend fun mutate(repository: Repository, args: List<String>): Either<GitError, Unit> = run(repository, args).map {}
    private fun failure(message: String): Either<GitError, Nothing> = GitError.CommandFailed("git", -1, message).left()

    override suspend fun branches(repository: Repository): Either<GitError, List<Branch>> {
        return failure("Branch listing is not available yet.")
    }

    override suspend fun commits(repository: Repository): Either<GitError, List<Commit>> {
        return failure("Commit history is not available yet.")
    }

    private companion object {
        const val MAX_DIFF_BYTES = 1_000_000L
        const val MAX_DIFF_ROWS = 5_000

    }
}
