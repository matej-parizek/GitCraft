package cz.parizmat.gitcraft.feature.toolwindow.viewmodel

import cz.parizmat.gitcraft.core.domain.element.Repository
import cz.parizmat.gitcraft.core.terminal.TerminalSessionFactory
import cz.parizmat.gitcraft.core.terminal.completion.CommandHistoryCompletionProvider
import cz.parizmat.gitcraft.core.terminal.completion.TerminalCompletionService
import cz.parizmat.gitcraft.feature.toolwindow.model.TerminalToolWindowState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.nio.file.Path

class TerminalToolWindowViewModel(
    private val sessionFactory: TerminalSessionFactory,
    val completionService: TerminalCompletionService,
    val commandHistory: CommandHistoryCompletionProvider,
) : AutoCloseable {
    private val mutableState = MutableStateFlow(TerminalToolWindowState())
    val state = mutableState.asStateFlow()

    fun setRepository(repository: Repository?) {
        val root = repository?.rootPath?.normalize()
        if (root == mutableState.value.repositoryRoot) return
        replaceSession(root)
    }

    fun toggleExpanded() {
        mutableState.update { it.copy(isExpanded = !it.isExpanded) }
    }

    fun restart() {
        replaceSession(mutableState.value.repositoryRoot)
    }

    fun resizeBy(dragAmount: Float) {
        mutableState.update {
            it.copy(height = (it.height - dragAmount).coerceIn(MIN_HEIGHT, MAX_HEIGHT))
        }
    }

    override fun close() {
        mutableState.value.session?.close()
        mutableState.update { it.copy(session = null) }
    }

    private fun replaceSession(root: Path?) {
        mutableState.value.session?.close()
        if (root == null) {
            mutableState.update { it.copy(repositoryRoot = null, session = null, error = null) }
            return
        }
        runCatching { sessionFactory.create(root) }.fold(
            onSuccess = { session -> mutableState.update { it.copy(repositoryRoot = root, session = session, error = null) } },
            onFailure = { error -> mutableState.update { it.copy(repositoryRoot = root, session = null, error = error.message ?: "PowerShell could not be started.") } },
        )
    }

    companion object {
        private const val MIN_HEIGHT = 180f
        private const val MAX_HEIGHT = 520f
    }
}
