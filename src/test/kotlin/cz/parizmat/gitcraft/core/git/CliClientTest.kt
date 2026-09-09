package cz.parizmat.gitcraft.core.git

import arrow.core.Either
import cz.parizmat.gitcraft.core.domain.element.DiffRowType
import cz.parizmat.gitcraft.core.domain.element.Repository
import cz.parizmat.gitcraft.core.domain.element.enums.FileStatus
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CliClientTest {
    private val client = CliClient(CommandExecutor())

    @Test fun stagesPartialChangesAndLiteralPaths() = runBlocking {
        val repository = fixture()
        val file = repository.rootPath.resolve("[literal] name.txt")
        Files.writeString(file, "initial\n")
        success(client.stageAll(repository))
        success(client.commit(repository, "Initial", false))
        Files.writeString(file, "staged\n")
        success(client.stageFile(repository, success(client.status(repository)).single()))
        Files.writeString(file, "working\n")
        val change = success(client.status(repository)).single()
        assertEquals(FileStatus.MODIFIED, change.indexStatus)
        assertEquals(FileStatus.MODIFIED, change.workingTreeStatus)
        assertTrue(success(client.diff(repository, change, true)).rows.any { it.type == DiffRowType.ADDED && it.text == "staged" })
        assertTrue(success(client.diff(repository, change, false)).rows.any { it.type == DiffRowType.ADDED && it.text == "working" })
        success(client.unstageFile(repository, change))
        assertEquals(FileStatus.UNMODIFIED, success(client.status(repository)).single().indexStatus)
        assertEquals("working\n", Files.readString(file))
    }

    @Test fun unstageWorksBeforeFirstCommit() = runBlocking {
        val repository = fixture()
        Files.writeString(repository.rootPath.resolve("new.txt"), "new\n")
        assertFalse(success(client.hasCommits(repository)))
        success(client.stageAll(repository))
        success(client.unstageAll(repository))
        assertEquals(FileStatus.UNTRACKED, success(client.status(repository)).single().indexStatus)
        assertTrue(client.discardChanges(repository) is Either.Left)
        assertTrue(Files.exists(repository.rootPath.resolve("new.txt")))
    }

    @Test fun discardRestoresTrackedAndPreservesIgnoredFiles() = runBlocking {
        val repository = fixture()
        Files.writeString(repository.rootPath.resolve(".gitignore"), "secret.local\n")
        Files.writeString(repository.rootPath.resolve("tracked.txt"), "original\n")
        success(client.stageAll(repository))
        success(client.commit(repository, "Initial", false))
        Files.writeString(repository.rootPath.resolve("tracked.txt"), "changed\n")
        Files.writeString(repository.rootPath.resolve("added.txt"), "added\n")
        success(client.stageAll(repository))
        Files.writeString(repository.rootPath.resolve("untracked.txt"), "untracked\n")
        Files.writeString(repository.rootPath.resolve("secret.local"), "preserve\n")
        success(client.discardChanges(repository))
        assertEquals("original\n", Files.readString(repository.rootPath.resolve("tracked.txt")))
        assertFalse(Files.exists(repository.rootPath.resolve("added.txt")))
        assertFalse(Files.exists(repository.rootPath.resolve("untracked.txt")))
        assertEquals("preserve\n", Files.readString(repository.rootPath.resolve("secret.local")))
        assertTrue(success(client.status(repository)).isEmpty())
    }

    @Test fun stagedRenameRetainsBothNames() = runBlocking {
        val repository = fixture()
        Files.writeString(repository.rootPath.resolve("before.txt"), "original\n")
        success(client.stageAll(repository))
        success(client.commit(repository, "Initial", false))
        Files.move(repository.rootPath.resolve("before.txt"), repository.rootPath.resolve("after.txt"))
        success(client.stageAll(repository))
        val change = success(client.status(repository)).single()
        assertEquals(Path.of("before.txt"), change.oldPath)
        assertEquals(Path.of("after.txt"), change.path)
        success(client.unstageFile(repository, change))
        assertTrue(success(client.status(repository)).all { it.indexStatus in setOf(FileStatus.UNMODIFIED, FileStatus.UNTRACKED) })
    }

    @Test fun binaryMetadataPhrasesInTextRemainVisibleAsTextDiffs() = runBlocking {
        val repository = fixture()
        val file = repository.rootPath.resolve("text.txt")
        Files.writeString(file, "original\n")
        success(client.stageAll(repository))
        success(client.commit(repository, "Initial", false))
        Files.writeString(file, "Binary files old and new differ\nGIT binary patch\n")
        val change = success(client.status(repository)).single()
        val diff = success(client.diff(repository, change, false))
        assertFalse(diff.isBinary)
        assertTrue(diff.rows.any { it.type == DiffRowType.ADDED && it.text == "Binary files old and new differ" })
        assertTrue(diff.rows.any { it.type == DiffRowType.ADDED && it.text == "GIT binary patch" })
    }

    @Test fun binaryAndLargeUntrackedFilesHaveBoundedPreviews() = runBlocking {
        val repository = fixture()
        Files.write(repository.rootPath.resolve("binary.bin"), byteArrayOf(0, 1, 2))
        Files.writeString(repository.rootPath.resolve("large.txt"), "x".repeat(1_000_001))
        val changes = success(client.status(repository))
        val binary = changes.single { it.path.toString() == "binary.bin" }
        val large = changes.single { it.path.toString() == "large.txt" }
        assertTrue(success(client.diff(repository, binary, false)).isBinary)
        assertTrue(success(client.diff(repository, large, false)).isTruncated)
        success(client.stageFile(repository, binary))
        val stagedBinary = success(client.status(repository)).single { it.path.toString() == "binary.bin" }
        assertTrue(success(client.diff(repository, stagedBinary, true)).isBinary)
    }

    @Test fun discardRefusesNestedRepositoryBeforeChangingTrackedFiles() = runBlocking {
        val repository = fixture()
        val tracked = repository.rootPath.resolve("tracked.txt")
        Files.writeString(tracked, "original\n")
        success(client.stageAll(repository))
        success(client.commit(repository, "Initial", false))
        Files.writeString(tracked, "preserve\n")
        val nested = Files.createDirectory(repository.rootPath.resolve("nested"))
        success(CommandExecutor().execute(nested, listOf("init", "-q")))
        Files.writeString(nested.resolve("new.txt"), "preserve nested\n")
        assertTrue(client.discardChanges(repository) is Either.Left)
        assertEquals("preserve\n", Files.readString(tracked))
        assertTrue(Files.exists(nested.resolve("new.txt")))
    }

    @Test fun commitRejectsEmptyMessageAndEmptyIndexIncludingAmend() = runBlocking {
        val repository = fixture()
        val file = repository.rootPath.resolve("tracked.txt")
        Files.writeString(file, "initial\n")
        assertTrue(client.commit(repository, "Initial", false) is Either.Left)
        success(client.stageAll(repository))
        assertTrue(client.commit(repository, "  \n", false) is Either.Left)
        assertFalse(success(client.hasCommits(repository)))
        success(client.commit(repository, "Initial", false))
        val executor = CommandExecutor()
        val before = success(executor.execute(repository.rootPath, listOf("rev-parse", "HEAD"))).stdout
        assertTrue(client.commit(repository, "Replacement", true) is Either.Left)
        assertEquals(before, success(executor.execute(repository.rootPath, listOf("rev-parse", "HEAD"))).stdout)
        Files.writeString(file, "amended\n")
        success(client.stageAll(repository))
        success(client.commit(repository, "Amended", true))
        assertEquals("1", success(executor.execute(repository.rootPath, listOf("rev-list", "--count", "HEAD"))).stdout.trim())
    }

    @Test fun conflictBlocksCommitAndDiscardAtServiceBoundary() = runBlocking {
        val repository = fixture()
        val file = repository.rootPath.resolve("conflict.txt")
        val executor = CommandExecutor()
        Files.writeString(file, "base\n")
        success(client.stageAll(repository))
        success(client.commit(repository, "Base", false))
        success(executor.execute(repository.rootPath, listOf("checkout", "-b", "side")))
        Files.writeString(file, "side\n")
        success(client.stageAll(repository))
        success(client.commit(repository, "Side", false))
        success(executor.execute(repository.rootPath, listOf("checkout", requireNotNull(repository.currentBranch))))
        Files.writeString(file, "main\n")
        success(client.stageAll(repository))
        success(client.commit(repository, "Main", false))
        val merge = success(executor.execute(repository.rootPath, listOf("merge", "side"), setOf(0, 1)))
        assertEquals(1, merge.exitCode)
        assertEquals(FileStatus.CONFLICTED, success(client.status(repository)).single().indexStatus)
        val conflict = Files.readString(file)
        assertTrue(client.commit(repository, "Unresolved", false) is Either.Left)
        assertTrue(client.discardChanges(repository) is Either.Left)
        assertEquals(conflict, Files.readString(file))
    }

    private suspend fun fixture(): Repository {
        val root = Files.createTempDirectory("gitcraft-test-")
        val executor = CommandExecutor()
        success(executor.execute(root, listOf("init", "-q")))
        success(executor.execute(root, listOf("config", "user.name", "GitCraft Test")))
        success(executor.execute(root, listOf("config", "user.email", "gitcraft@example.invalid")))
        success(executor.execute(root, listOf("config", "commit.gpgsign", "false")))
        success(executor.execute(root, listOf("config", "core.autocrlf", "false")))
        return success(client.openRepository(root))
    }

    private fun <E, T> success(result: Either<E, T>): T = when (result) {
        is Either.Left -> error("Expected success: ${result.value}")
        is Either.Right -> result.value
    }
}
