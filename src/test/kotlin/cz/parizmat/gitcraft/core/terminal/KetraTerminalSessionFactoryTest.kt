package cz.parizmat.gitcraft.core.terminal

import io.github.ketraterm.input.event.TerminalKey
import io.github.ketraterm.input.event.TerminalKeyEvent
import io.github.ketraterm.input.event.TerminalModifiers
import io.github.ketraterm.input.event.TerminalPasteEvent
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KetraTerminalSessionFactoryTest {
    @Test
    fun `powershell session runs in repository directory and accepts terminal input`() {
        if (!System.getProperty("os.name").startsWith("Windows")) return
        val repositoryRoot = createTempDirectory("gitcraft-terminal-")
        val session = KetraTerminalSessionFactory().create(repositoryRoot)

        try {
            session.resize(columns = 120, rows = 30)
            assertEquals(120, session.terminal.width)
            assertEquals(30, session.terminal.height)

            runCommand(session, "Write-Output ((Get-Location).Path); Write-Output 'GITCRAFT_PTY_OK'")
            val output = awaitScreenText(session) { it.occurrencesOf("GITCRAFT_PTY_OK") >= 2 }

            assertTrue(repositoryRoot.toRealPath().toString() in output)
            assertTrue("GITCRAFT_PTY_OK" in output)

            runCommand(session, "Start-Sleep -Seconds 10")
            Thread.sleep(250)
            session.encodeKey(
                TerminalKeyEvent.codepoint(
                    codepoint = 'c'.code,
                    modifiers = TerminalModifiers.CTRL,
                ),
            )
            Thread.sleep(500)
            runCommand(session, "Write-Output 'GITCRAFT_INTERRUPT_OK'")
            assertTrue(
                "GITCRAFT_INTERRUPT_OK" in awaitScreenText(session) {
                    it.occurrencesOf("GITCRAFT_INTERRUPT_OK") >= 2
                },
            )
        } finally {
            session.close()
            repositoryRoot.toFile().deleteRecursively()
        }
    }

    private fun runCommand(session: io.github.ketraterm.session.TerminalSession, command: String) {
        session.encodePaste(TerminalPasteEvent(command))
        session.encodeKey(TerminalKeyEvent.key(TerminalKey.ENTER))
    }

    private fun awaitScreenText(
        session: io.github.ketraterm.session.TerminalSession,
        predicate: (String) -> Boolean,
    ): String {
        val deadline = System.nanoTime() + 5_000_000_000L
        var text = ""
        while (System.nanoTime() < deadline) {
            text = session.terminal.getAllAsString()
            if (predicate(text)) return text
            Thread.sleep(25)
        }
        error("Terminal output did not arrive in time. Last screen:\n$text")
    }

    private fun String.occurrencesOf(value: String): Int = windowed(value.length).count(value::equals)
}
