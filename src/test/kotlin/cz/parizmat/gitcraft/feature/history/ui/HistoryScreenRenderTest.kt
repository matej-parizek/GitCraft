package cz.parizmat.gitcraft.feature.history.ui

import androidx.compose.ui.awt.ComposePanel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cz.parizmat.gitcraft.core.git.CliClient
import cz.parizmat.gitcraft.core.git.CommandExecutor
import cz.parizmat.gitcraft.core.ui.theme.GitCraftTheme
import cz.parizmat.gitcraft.feature.history.viewmodel.HistoryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.Window
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HistoryScreenRenderTest {
    @Test
    fun `history screen renders real repository data and selection`() {
        if (GraphicsEnvironment.isHeadless()) return
        val git = CliClient(CommandExecutor())
        val repository = runBlocking {
            git.openRepository(Path.of(".").toAbsolutePath().normalize()).fold(
                { error("GitCraft repository is required for render QA: $it") },
                { it },
            )
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val viewModel = HistoryViewModel(git, scope)
        var frame: JFrame? = null
        lateinit var panel: ComposePanel

        try {
            SwingUtilities.invokeAndWait {
                panel = ComposePanel().apply {
                    setContent {
                        GitCraftTheme(darkTheme = true) {
                            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                                HistoryScreen(viewModel, onOpenRepository = {})
                            }
                        }
                    }
                }
                frame = JFrame("GitCraft History QA").apply {
                    isUndecorated = true
                    contentPane.add(panel)
                    setSize(1_200, 700)
                    setLocationRelativeTo(null)
                    isAlwaysOnTop = true
                    isVisible = true
                    toFront()
                }
            }
            viewModel.setRepository(repository)
            awaitCondition { !viewModel.state.value.loading && viewModel.state.value.details != null }
            capture(panel, Path.of("build", "visual-qa", "history-screen.png"))

            viewModel.openDiff()
            awaitCondition { viewModel.state.value.diffOpen && !viewModel.state.value.diffLoading && viewModel.state.value.diff != null }
            assertEquals(1, Window.getWindows().count { it.isShowing }, "Commit diff must stay inside the GitCraft window")
            assertTrue(viewModel.state.value.details?.commit?.shortHash?.isNotBlank() == true)
            capture(panel, Path.of("build", "visual-qa", "history-screen-diff.png"))
            viewModel.closeDiff()
            awaitCondition { !viewModel.state.value.diffOpen }

            val firstSelection = viewModel.state.value.selectedHash
            viewModel.moveSelection(1)
            awaitCondition { viewModel.state.value.selectedHash != firstSelection && !viewModel.state.value.detailsLoading }
            assertNotEquals(firstSelection, viewModel.state.value.selectedHash)

            SwingUtilities.invokeAndWait { checkNotNull(frame).setSize(800, 600) }
            capture(panel, Path.of("build", "visual-qa", "history-screen-compact.png"))
            SwingUtilities.invokeAndWait { checkNotNull(frame).setSize(1_200, 700) }

            viewModel.setQuery("terminal")
            awaitCondition { viewModel.state.value.query == "terminal" && viewModel.state.value.visibleRows.size < viewModel.state.value.rows.size }
            assertTrue(viewModel.state.value.visibleRows.isNotEmpty())
            capture(panel, Path.of("build", "visual-qa", "history-screen-filtered.png"))
        } finally {
            SwingUtilities.invokeAndWait { frame?.dispose() }
            scope.cancel()
        }
    }

    private fun awaitCondition(predicate: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (System.nanoTime() < deadline) {
            if (predicate()) return
            Thread.sleep(25)
        }
        error("History UI state did not settle in time")
    }

    private fun capture(panel: ComposePanel, path: Path) {
        Files.createDirectories(path.parent)
        repeat(5) {
            Thread.sleep(900)
            Toolkit.getDefaultToolkit().sync()
            var bounds: Rectangle? = null
            SwingUtilities.invokeAndWait {
                SwingUtilities.getWindowAncestor(panel)?.toFront()
                val location = panel.locationOnScreen
                bounds = Rectangle(location.x, location.y, panel.width, panel.height)
            }
            val image = Robot().createScreenCapture(checkNotNull(bounds))
            if (hasRenderedContent(image)) {
                ImageIO.write(image, "png", path.toFile())
                return
            }
        }
        error("Compose capture stayed blank after five attempts: $path")
    }

    private fun hasRenderedContent(image: java.awt.image.BufferedImage): Boolean {
        val colors = HashSet<Int>()
        for (y in 0 until image.height step 20) {
            for (x in 0 until image.width step 20) colors += image.getRGB(x, y)
        }
        return colors.size >= 8
    }
}
