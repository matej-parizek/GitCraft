package cz.parizmat.gitcraft.feature.toolwindow.model

import io.github.ketraterm.session.TerminalSession
import java.nio.file.Path

data class TerminalToolWindowState(
    val isExpanded: Boolean = true,
    val height: Float = DEFAULT_HEIGHT,
    val repositoryRoot: Path? = null,
    val session: TerminalSession? = null,
    val error: String? = null,
) {
    companion object {
        const val DEFAULT_HEIGHT = 280f
    }
}
