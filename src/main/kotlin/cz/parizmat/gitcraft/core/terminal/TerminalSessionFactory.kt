package cz.parizmat.gitcraft.core.terminal

import io.github.ketraterm.pty.PtyOptions
import io.github.ketraterm.pty.TerminalSessions
import io.github.ketraterm.session.TerminalSession
import java.nio.file.Path

fun interface TerminalSessionFactory {
    fun create(workingDirectory: Path): TerminalSession
}

class KetraTerminalSessionFactory : TerminalSessionFactory {
    override fun create(workingDirectory: Path): TerminalSession =
        TerminalSessions.localPty(
            PtyOptions(
                command = WINDOWS_POWERSHELL,
                workingDirectory = workingDirectory,
                columns = INITIAL_COLUMNS,
                rows = INITIAL_ROWS,
                maxHistory = MAX_HISTORY,
                readerThreadName = "gitcraft-terminal-reader",
                watcherThreadName = "gitcraft-terminal-watcher",
            ),
        )

    private companion object {
        val WINDOWS_POWERSHELL = listOf("powershell.exe", "-NoLogo")
        const val INITIAL_COLUMNS = 100
        const val INITIAL_ROWS = 20
        const val MAX_HISTORY = 5_000
    }
}
