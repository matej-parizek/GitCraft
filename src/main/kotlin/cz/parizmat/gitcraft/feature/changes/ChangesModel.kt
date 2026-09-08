package cz.parizmat.gitcraft.feature.changes

import arrow.core.Either
import cz.parizmat.gitcraft.core.domain.base.GitError
import cz.parizmat.gitcraft.core.domain.base.ProcessError
import cz.parizmat.gitcraft.core.domain.element.FileDiff
import cz.parizmat.gitcraft.core.domain.element.GitChange
import cz.parizmat.gitcraft.core.domain.element.Repository
import cz.parizmat.gitcraft.core.domain.element.enums.FileStatus
import cz.parizmat.gitcraft.core.git.GitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Path

data class FileSelection(val change: GitChange, val staged: Boolean)
data class ChangesUiState(
    val repository: Repository? = null,
    val changes: List<GitChange> = emptyList(),
    val selection: FileSelection? = null,
    val diff: FileDiff? = null,
    val diffLoading: Boolean = false,
    val diffError: String? = null,
    val busy: Boolean = false,
    val hasCommits: Boolean = false,
    val message: String = "",
    val amend: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
) {
    val staged get() = changes.filter { it.indexStatus !in setOf(FileStatus.UNMODIFIED, FileStatus.UNTRACKED, FileStatus.IGNORED) }
    val unstaged get() = changes.filter { it.workingTreeStatus !in setOf(FileStatus.UNMODIFIED, FileStatus.IGNORED) }
    val conflicted get() = changes.any { it.indexStatus == FileStatus.CONFLICTED || it.workingTreeStatus == FileStatus.CONFLICTED }
    val canCommit get() = repository != null && staged.isNotEmpty() && message.isNotBlank() && !busy && !conflicted
}

class ChangesModel(private val git: GitClient, private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow(ChangesUiState())
    val state = mutableState.asStateFlow()
    private val operations = Mutex()
    private var diffJob: Job? = null
    private var refreshJob: Job? = null
    private var diffVersion = 0L

    fun open(path: String) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, error = null) }
        scope.launch {
            try {
                operations.withLock {
                    val parsedPath = try { Path.of(path) } catch (e: IllegalArgumentException) {
                        mutableState.update { it.copy(error = "Invalid repository path: ${e.message}") }
                        return@withLock
                    }
                    git.openRepository(parsedPath).fold(
                        { error -> mutableState.update { it.copy(error = error.describe()) } },
                        { repository ->
                            diffJob?.cancel()
                            diffVersion++
                            mutableState.value = ChangesUiState(repository = repository, busy = true)
                            refreshLocked(repository)
                        },
                    )
                }
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    fun refresh() {
        val repository = state.value.repository ?: return
        if (state.value.busy || refreshJob?.isActive == true) return
        refreshJob = scope.launch { operations.withLock { if (state.value.repository == repository) refreshLocked(repository) } }
    }

    private suspend fun refreshLocked(repository: Repository) {
        git.status(repository).fold(
            { error -> mutableState.update { it.copy(error = error.describe()) } },
            { changes ->
                val previous = state.value.selection
                val matching = previous?.let { selection ->
                    changes.find { it.path == selection.change.path }?.let { updated ->
                        val status = if (selection.staged) updated.indexStatus else updated.workingTreeStatus
                        if (status != FileStatus.UNMODIFIED && status != FileStatus.IGNORED && !(selection.staged && status == FileStatus.UNTRACKED))
                            FileSelection(updated, selection.staged) else null
                    }
                }
                mutableState.update { it.copy(changes = changes, selection = matching) }
                if (matching == null) {
                    diffJob?.cancel()
                    diffVersion++
                    mutableState.update { it.copy(diff = null, diffLoading = false, diffError = null) }
                } else loadDiff(matching, background = true)
            },
        )
        git.hasCommits(repository).fold(
            { error -> mutableState.update { it.copy(error = error.describe()) } },
            { hasCommits -> mutableState.update { it.copy(hasCommits = hasCommits, amend = it.amend && hasCommits) } },
        )
        git.openRepository(repository.rootPath).onRight { updated ->
            mutableState.update { it.copy(repository = updated) }
        }
    }

    fun select(change: GitChange, staged: Boolean) {
        val selection = FileSelection(change, staged)
        mutableState.update { it.copy(selection = selection, diff = null, diffError = null) }
        loadDiff(selection)
    }

    private fun loadDiff(selection: FileSelection, background: Boolean = false) {
        val repository = state.value.repository ?: return
        if (background && diffJob?.isActive == true) return
        diffJob?.cancel()
        val version = ++diffVersion
        if (!background) mutableState.update { it.copy(diffLoading = true) }
        diffJob = scope.launch {
            git.diff(repository, selection.change, selection.staged).fold(
                { error -> if (version == diffVersion) mutableState.update { it.copy(diffLoading = false, diffError = error.describe()) } },
                { diff -> if (version == diffVersion) mutableState.update { it.copy(diff = diff, diffLoading = false, diffError = null) } },
            )
        }
    }

    fun setMessage(message: String) { mutableState.update { it.copy(message = message) } }
    fun setAmend(amend: Boolean) { mutableState.update { it.copy(amend = amend && it.hasCommits) } }
    fun dismissFeedback() { mutableState.update { it.copy(error = null, notice = null) } }
    fun stage(change: GitChange) = mutate { git.stageFile(it, change) }
    fun unstage(change: GitChange) = mutate { git.unstageFile(it, change) }
    fun stageAll() = mutate { git.stageAll(it) }
    fun unstageAll() = mutate { git.unstageAll(it) }
    fun discardAll() = mutate(success = "Uncommitted changes discarded.") { git.discardChanges(it) }
    fun commit() {
        val current = state.value
        if (!current.canCommit) return
        mutate(success = if (current.amend) "Latest commit replaced locally." else "Commit created locally. Nothing was pushed.", clearDraft = true) {
            git.commit(it, current.message.trim(), current.amend)
        }
    }

    private fun mutate(success: String? = null, clearDraft: Boolean = false, operation: suspend (Repository) -> Either<GitError, Unit>) {
        val repository = state.value.repository ?: return
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, error = null, notice = null) }
        scope.launch {
            try {
                operations.withLock {
                    operation(repository).fold(
                        { error -> mutableState.update { it.copy(error = if (clearDraft) "Commit failed. Your commit message was preserved. ${error.describe()}" else error.describe()) } },
                        { mutableState.update { it.copy(notice = success, message = if (clearDraft) "" else it.message, amend = if (clearDraft) false else it.amend) } },
                    )
                    refreshLocked(repository)
                }
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }
}

private fun GitError.describe(): String = when (this) {
    GitError.NotGitRepository -> "This folder is not a Git repository. Open a repository to continue."
    is GitError.CommandFailed -> message.ifBlank { "Git failed with exit code $exitCode." }
    is GitError.Unexpected -> cause.message ?: "Git could not complete this operation."
}
private fun ProcessError.describe(): String = when (this) {
    is ProcessError.CommandFailed -> stderr.ifBlank { "Could not open this Git repository." }
    is ProcessError.ProcessStartFailed -> "Could not start Git. Check that Git is installed and available on PATH."
    is ProcessError.ProcessInterrupted -> "Opening the repository was interrupted."
}
