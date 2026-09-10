package cz.parizmat.gitcraft.feature.history.viewmodel

import arrow.core.Either
import cz.parizmat.gitcraft.core.domain.base.GitError
import cz.parizmat.gitcraft.core.domain.element.Commit
import cz.parizmat.gitcraft.core.domain.element.CommitDetails
import cz.parizmat.gitcraft.core.domain.element.Repository
import cz.parizmat.gitcraft.core.git.GitClient
import cz.parizmat.gitcraft.feature.history.model.CommitGraphLayout
import cz.parizmat.gitcraft.feature.history.model.CommitGraphRow
import cz.parizmat.gitcraft.feature.history.model.HistoryUiState
import cz.parizmat.gitcraft.feature.history.model.HistoryRef
import cz.parizmat.gitcraft.feature.history.model.HistoryRefType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HistoryViewModel(
    private val git: GitClient,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(HistoryUiState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null
    private var detailsJob: Job? = null
    private var diffJob: Job? = null
    private var generation = 0L
    private val detailsCache = object : LinkedHashMap<String, CommitDetails>(DETAIL_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CommitDetails>?): Boolean = size > DETAIL_CACHE_SIZE
    }

    fun setRepository(repository: Repository?) {
        if (repository?.rootPath == state.value.repository?.rootPath) return
        generation++
        loadJob?.cancel()
        detailsJob?.cancel()
        diffJob?.cancel()
        detailsCache.clear()
        mutableState.value = HistoryUiState(repository = repository)
        if (repository != null) refresh()
    }

    fun refresh() {
        val repository = state.value.repository ?: return
        if (loadJob?.isActive == true) return
        val requestGeneration = ++generation
        mutableState.update { it.copy(loading = true, error = null) }
        loadJob = scope.launch {
            val commitsRequest = async { git.commits(repository) }
            val branchesRequest = async { git.branches(repository) }
            val commitsResult = commitsRequest.await()
            val branchesResult = branchesRequest.await()
            if (requestGeneration != generation || state.value.repository?.rootPath != repository.rootPath) return@launch

            when (commitsResult) {
                is Either.Left -> mutableState.update { it.copy(loading = false, error = commitsResult.value.describe()) }
                is Either.Right -> {
                    val rows = withContext(Dispatchers.Default) { CommitGraphLayout.layout(commitsResult.value) }
                    val branches = branchesResult.fold({ emptyList() }, { it })
                    val branchError = when (branchesResult) {
                        is Either.Left -> branchesResult.value.describe()
                        is Either.Right -> null
                    }
                    val previousSelection = state.value.selectedHash
                    val selected = rows.firstOrNull { it.commit.hash == previousSelection } ?: rows.firstOrNull()
                    mutableState.update {
                        it.copy(
                            rows = rows,
                            branches = branches,
                            selectedHash = selected?.commit?.hash,
                            loading = false,
                            error = branchError,
                        ).filtered()
                    }
                    selected?.commit?.let(::loadDetails)
                }
            }
        }
    }

    fun setQuery(query: String) {
        mutableState.update { it.copy(query = query).filtered() }
        keepSelectionVisible()
    }

    fun selectRef(ref: HistoryRef?) {
        mutableState.update { it.copy(selectedRef = ref).filtered() }
        keepSelectionVisible()
    }

    fun openDiff() {
        val repository = state.value.repository ?: return
        val commit = state.value.details?.commit ?: return
        diffJob?.cancel()
        val requestGeneration = generation
        mutableState.update { it.copy(diffOpen = true, diffLoading = true, diff = null) }
        diffJob = scope.launch {
            git.commitDiff(repository, commit).fold(
                { error ->
                    if (requestGeneration == generation && state.value.selectedHash == commit.hash) {
                        mutableState.update { it.copy(diffOpen = false, diffLoading = false, error = error.describe()) }
                    }
                },
                { diff ->
                    if (requestGeneration == generation && state.value.selectedHash == commit.hash) {
                        mutableState.update { it.copy(diff = diff, diffLoading = false) }
                    }
                },
            )
        }
    }

    fun closeDiff() {
        diffJob?.cancel()
        mutableState.update { it.copy(diffOpen = false, diffLoading = false, diff = null) }
    }

    fun moveSelection(offset: Int) {
        val current = state.value
        if (current.visibleRows.isEmpty()) return
        val selectedIndex = current.visibleRows.indexOfFirst { it.commit.hash == current.selectedHash }
        val targetIndex = (if (selectedIndex < 0) 0 else selectedIndex + offset).coerceIn(current.visibleRows.indices)
        selectCommit(current.visibleRows[targetIndex].commit)
    }

    fun selectCommit(commit: Commit) {
        if (commit.hash == state.value.selectedHash && state.value.details != null) return
        mutableState.update { it.copy(selectedHash = commit.hash, details = detailsCache[commit.hash], detailsLoading = commit.hash !in detailsCache) }
        if (commit.hash !in detailsCache) loadDetails(commit)
    }

    private fun loadDetails(commit: Commit) {
        val repository = state.value.repository ?: return
        detailsJob?.cancel()
        val requestGeneration = generation
        mutableState.update { it.copy(detailsLoading = commit.hash !in detailsCache) }
        detailsCache[commit.hash]?.let { cached ->
            mutableState.update { it.copy(details = cached, detailsLoading = false) }
            return
        }
        detailsJob = scope.launch {
            git.commitDetails(repository, commit).fold(
                { error ->
                    if (requestGeneration == generation && state.value.selectedHash == commit.hash) {
                        mutableState.update { it.copy(detailsLoading = false, error = error.describe()) }
                    }
                },
                { details ->
                    detailsCache[commit.hash] = details
                    if (requestGeneration == generation && state.value.selectedHash == commit.hash) {
                        mutableState.update { it.copy(details = details, detailsLoading = false) }
                    }
                },
            )
        }
    }

    private fun HistoryUiState.filtered(): HistoryUiState {
        val normalized = query.trim()
        val selectedRoot = selectedRef?.let { ref ->
            when (ref.type) {
                HistoryRefType.BRANCH -> branches.firstOrNull { it.name == ref.name }?.lastCommit
                HistoryRefType.TAG -> rows.firstOrNull { ref.name in it.commit.tags }?.commit?.hash
            }
        }
        val reachable = selectedRoot?.let { root -> reachableFrom(root) }
        val filtered = rows.filter { row ->
            val commit = row.commit
            val matchesRef = selectedRef == null || commit.hash in reachable.orEmpty()
            val matchesQuery = normalized.isEmpty() || sequenceOf(
                commit.message,
                commit.body,
                commit.shortHash,
                commit.hash,
                commit.authorName,
                commit.authorEmail,
                commit.tags.joinToString(),
                commit.branch.joinToString { it.name },
            ).any { it.contains(normalized, ignoreCase = true) }
            matchesRef && matchesQuery
        }
        return copy(visibleRows = CommitGraphLayout.layout(filtered.map { it.commit }))
    }

    private fun HistoryUiState.reachableFrom(root: String): Set<String> {
        val commits = rows.associateBy { it.commit.hash }
        val reachable = mutableSetOf<String>()
        val pending = ArrayDeque<String>().apply { add(root) }
        while (pending.isNotEmpty()) {
            val hash = pending.removeLast()
            if (!reachable.add(hash)) continue
            commits[hash]?.commit?.parentsHashes?.forEach(pending::add)
        }
        return reachable
    }

    private fun keepSelectionVisible() {
        val current = state.value
        if (current.visibleRows.isEmpty()) {
            detailsJob?.cancel()
            mutableState.update { it.copy(selectedHash = null, details = null, detailsLoading = false) }
            return
        }
        if (current.visibleRows.none { it.commit.hash == current.selectedHash }) {
            current.visibleRows.firstOrNull()?.commit?.let(::selectCommit)
        }
    }

    private companion object {
        const val DETAIL_CACHE_SIZE = 24
    }
}

private fun GitError.describe(): String = when (this) {
    GitError.NotGitRepository -> "This folder is not a Git repository."
    is GitError.CommandFailed -> message.ifBlank { "Git failed with exit code $exitCode." }
    is GitError.Unexpected -> cause.message ?: "Git could not load history."
}
