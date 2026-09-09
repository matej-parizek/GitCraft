package cz.parizmat.gitcraft.feature.toolwindow.ui

import cz.parizmat.gitcraft.core.terminal.KetraTerminalSessionFactory
import cz.parizmat.gitcraft.core.terminal.completion.CommandHistoryCompletionProvider
import cz.parizmat.gitcraft.core.terminal.completion.CompletionCandidate
import cz.parizmat.gitcraft.core.terminal.completion.CompletionProvider
import cz.parizmat.gitcraft.core.terminal.completion.CompletionType
import cz.parizmat.gitcraft.core.terminal.completion.TerminalCompletionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.awt.GraphicsEnvironment
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JWindow
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertTrue

class TerminalSuggestionPopupWindowTest {
    @Test
    fun `popup flips around the caret and renders outside the SwingPanel boundary`() {
        if (GraphicsEnvironment.isHeadless() || !System.getProperty("os.name").startsWith("Windows")) return
        val session = KetraTerminalSessionFactory().create(Path.of(".").toAbsolutePath().normalize())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val completionService = TerminalCompletionService(
            listOf(
                CompletionProvider {
                    (1..8).map { index ->
                        CompletionCandidate("suggestion-$index", CompletionType.COMMAND, "Completion description $index")
                    }
                },
            ),
        )
        val host = TerminalSwingHost(session, Path.of("."), completionService, CommandHistoryCompletionProvider(), scope)
        var frame: JFrame? = null
        lateinit var terminal: JComponent

        try {
            val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
            SwingUtilities.invokeAndWait {
                frame = JFrame("GitCraft popup QA").apply {
                    isUndecorated = true
                    terminal = host.createComponent()
                    contentPane.add(terminal)
                    setBounds(screen.x + 100, screen.y + screen.height - 150, 640, 120)
                    isVisible = true
                }
            }
            awaitCondition { session.terminal.cursorCol > 0 }
            SwingUtilities.invokeAndWait { openSuggestions(terminal) }
            val owner = checkNotNull(frame)
            val popup = awaitPopup(owner)
            assertTrue(popup.y < owner.y, "Popup should open above a caret near the screen bottom")
            assertTrue(popup.x > owner.x + 8, "Popup should align with the rendered caret, not the terminal edge")
            capture(owner, popup, Path.of("build", "visual-qa", "suggestion-popup-above.png"))

            SwingUtilities.invokeAndWait {
                owner.setLocation(screen.x + 100, screen.y + 24)
                openSuggestions(terminal)
            }
            awaitCondition { popup.y >= owner.y }
            assertTrue(popup.y >= owner.y, "Popup should open below a caret near the screen top")
            capture(owner, popup, Path.of("build", "visual-qa", "suggestion-popup-below.png"))
        } finally {
            SwingUtilities.invokeAndWait { frame?.dispose() }
            host.close()
            session.close()
            scope.cancel()
        }
    }

    private fun openSuggestions(terminal: JComponent) {
        val event = KeyEvent(
            terminal,
            KeyEvent.KEY_PRESSED,
            System.currentTimeMillis(),
            InputEvent.CTRL_DOWN_MASK,
            KeyEvent.VK_SPACE,
            ' ',
        )
        terminal.keyListeners.forEach { listener -> listener.keyPressed(event) }
    }

    private fun awaitPopup(owner: JFrame): JWindow {
        var popup: JWindow? = null
        awaitCondition {
            popup = java.awt.Window.getWindows().filterIsInstance<JWindow>()
                .firstOrNull { it.owner === owner && it.isVisible }
            popup != null
        }
        return checkNotNull(popup)
    }

    private fun awaitCondition(predicate: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (System.nanoTime() < deadline) {
            if (predicate()) return
            Thread.sleep(25)
        }
        error("Popup state did not settle in time")
    }

    private fun capture(owner: JFrame, popup: JWindow, path: Path) {
        Files.createDirectories(path.parent)
        SwingUtilities.invokeAndWait {
            val bounds = owner.bounds.union(popup.bounds)
            val image = BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            try {
                graphics.translate(owner.x - bounds.x, owner.y - bounds.y)
                owner.paintAll(graphics)
                graphics.translate(popup.x - owner.x, popup.y - owner.y)
                popup.paintAll(graphics)
            } finally {
                graphics.dispose()
            }
            ImageIO.write(image, "png", path.toFile())
        }
    }
}
