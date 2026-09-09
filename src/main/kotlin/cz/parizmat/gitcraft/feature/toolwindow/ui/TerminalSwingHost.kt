package cz.parizmat.gitcraft.feature.toolwindow.ui

import androidx.compose.ui.graphics.toArgb
import cz.parizmat.gitcraft.core.ui.theme.DarkBackground
import cz.parizmat.gitcraft.core.ui.theme.DarkBorder
import cz.parizmat.gitcraft.core.ui.theme.DarkTextPrimary
import cz.parizmat.gitcraft.core.ui.theme.GitCraftPurple
import cz.parizmat.gitcraft.core.ui.theme.LightTextPrimary
import cz.parizmat.gitcraft.core.terminal.completion.*
import cz.parizmat.gitcraft.feature.toolwindow.model.CommandLineTracker
import cz.parizmat.gitcraft.feature.toolwindow.model.SuggestionPopupPositioner
import io.github.ketraterm.input.event.TerminalKey
import io.github.ketraterm.input.event.TerminalKeyEvent
import io.github.ketraterm.input.event.TerminalPasteEvent
import io.github.ketraterm.render.api.TerminalColorPalette
import io.github.ketraterm.render.api.TerminalRenderBufferKind
import io.github.ketraterm.session.TerminalSession
import io.github.ketraterm.ui.swing.api.SwingHostServices
import io.github.ketraterm.ui.swing.api.SwingTerminal
import io.github.ketraterm.ui.swing.settings.SwingSettings
import io.github.ketraterm.ui.swing.settings.TerminalTheme
import io.github.ketraterm.ui.swing.suggestion.SwingShellSuggestion
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.HierarchyBoundsAdapter
import java.awt.event.HierarchyEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JWindow
import javax.swing.SwingUtilities

class TerminalSwingHost(
    private val session: TerminalSession,
    private val repositoryRoot: Path,
    private val completionService: TerminalCompletionService,
    private val history: CommandHistoryCompletionProvider,
    private val scope: CoroutineScope,
) : AutoCloseable {
    private val terminalSettings = createSettings()
    private val tracker = CommandLineTracker()
    private var requestJob: Job? = null
    private var generation = 0L
    private var candidatesByText: Map<String, CompletionCandidate> = emptyMap()
    private var suggestionPopup: JComponent? = null
    private var suggestionWindow: JWindow? = null
    private lateinit var terminal: SwingTerminal

    fun createComponent(): JComponent {
        val hostServices = SwingHostServices(
            scrollbarOverlayEnabled = false,
            hostKeyHandler = { event -> handleKeyPressed(event) },
            shellSuggestionHandler = { acceptance -> accept(acceptance.suggestion.replacementText) },
        )
        terminal = SwingTerminal(settingsProvider = { terminalSettings }, hostServices = hostServices)
        terminal.border = null
        suggestionPopup = terminal.components.filterIsInstance<JComponent>().singleOrNull()?.also { popup ->
            terminal.remove(popup)
            popup.font = terminalSettings.font
        }
        terminal.addKeyListener(object : KeyAdapter() {
            override fun keyTyped(event: KeyEvent) {
                if (isAlternateScreen()) {
                    tracker.clear()
                    cancelSuggestions()
                    return
                }
                if (!event.isControlDown && !event.isAltDown && event.keyChar != KeyEvent.CHAR_UNDEFINED) {
                    tracker.append(event.keyChar)
                    requestSuggestions(manual = false)
                }
            }
        })
        terminal.addFocusListener(object : FocusAdapter() {
            override fun focusLost(event: FocusEvent) = cancelSuggestions()
        })
        terminal.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) = positionSuggestionWindow()
        })
        terminal.addHierarchyBoundsListener(object : HierarchyBoundsAdapter() {
            override fun ancestorMoved(event: HierarchyEvent) = positionSuggestionWindow()

            override fun ancestorResized(event: HierarchyEvent) = positionSuggestionWindow()
        })
        terminal.bind(session)
        SwingUtilities.invokeLater { terminal.requestFocusInWindow() }
        return terminal
    }

    override fun close() {
        requestJob?.cancel()
        SwingUtilities.invokeLater {
            suggestionWindow?.dispose()
            suggestionWindow = null
            if (::terminal.isInitialized) terminal.dispose()
        }
    }

    private fun handleKeyPressed(event: KeyEvent): Boolean {
        if (isAlternateScreen()) {
            tracker.clear()
            cancelSuggestions()
            return false
        }
        val popupVisible = ::terminal.isInitialized && terminal.currentShellSuggestionState().visible
        if (event.keyCode == KeyEvent.VK_SPACE && event.isControlDown) {
            requestSuggestions(manual = true)
            return true
        }
        when (event.keyCode) {
            KeyEvent.VK_ENTER -> {
                if (popupVisible) terminal.hideShellSuggestions()
                history.record(tracker.clear())
                cancelSuggestions()
            }
            KeyEvent.VK_BACK_SPACE -> {
                tracker.backspace()
                requestSuggestions(manual = false)
            }
            KeyEvent.VK_C -> if (event.isControlDown) {
                tracker.clear()
                cancelSuggestions()
            }
            KeyEvent.VK_TAB -> if (!popupVisible) {
                tracker.clear()
                cancelSuggestions()
            }
            KeyEvent.VK_UP, KeyEvent.VK_DOWN -> if (!popupVisible) {
                tracker.clear()
                cancelSuggestions()
            }
            KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_HOME, KeyEvent.VK_END, KeyEvent.VK_DELETE -> {
                tracker.clear()
                cancelSuggestions()
            }
            KeyEvent.VK_ESCAPE -> if (popupVisible) {
                cancelSuggestions()
                return true
            }
        }
        return false
    }

    private fun requestSuggestions(manual: Boolean) {
        if (!::terminal.isInitialized) return
        if (isAlternateScreen()) {
            tracker.clear()
            cancelSuggestions()
            return
        }
        val line = tracker.commandLine
        if (!manual && line.isBlank()) {
            cancelSuggestions()
            return
        }
        requestJob?.cancel()
        val requestGeneration = ++generation
        requestJob = scope.launch {
            if (!manual) delay(AUTO_COMPLETE_DELAY_MILLIS)
            val request = CompletionRequest(line, line.length, repositoryRoot)
            val candidates = completionService.complete(request)
            if (requestGeneration != generation || tracker.commandLine != line) return@launch
            SwingUtilities.invokeLater {
                if (requestGeneration != generation || !::terminal.isInitialized) return@invokeLater
                candidatesByText = candidates.associateBy(CompletionCandidate::text)
                if (candidates.isEmpty()) {
                    terminal.hideShellSuggestions()
                    hideSuggestionWindow()
                } else {
                    terminal.showShellSuggestions(
                        suggestions = candidates.map { candidate ->
                            SwingShellSuggestion(
                                replacementText = candidate.text,
                                displayText = candidate.text,
                                detail = candidate.description,
                                source = candidate.type.label,
                            )
                        },
                        anchorColumn = session.terminal.cursorCol,
                        anchorRow = session.terminal.cursorRow,
                    )
                    SwingUtilities.invokeLater(::showSuggestionWindow)
                }
            }
        }
    }

    private fun accept(text: String) {
        val candidate = candidatesByText[text] ?: return
        val accepted = tracker.accept(candidate, repositoryRoot)
        repeat(accepted.removedCharacters) {
            session.encodeKey(TerminalKeyEvent.key(TerminalKey.BACKSPACE))
        }
        session.encodePaste(TerminalPasteEvent(accepted.insertedText))
        candidatesByText = emptyMap()
        generation++
        hideSuggestionWindow()
    }

    private fun cancelSuggestions() {
        requestJob?.cancel()
        generation++
        candidatesByText = emptyMap()
        if (::terminal.isInitialized) terminal.hideShellSuggestions()
        hideSuggestionWindow()
    }

    private fun isAlternateScreen(): Boolean {
        var activeBuffer = TerminalRenderBufferKind.PRIMARY
        session.readRenderFrame { frame -> activeBuffer = frame.activeBuffer }
        return activeBuffer == TerminalRenderBufferKind.ALTERNATE
    }

    private fun showSuggestionWindow() {
        val popup = suggestionPopup ?: return
        if (!terminal.currentShellSuggestionState().visible || !terminal.isShowing) {
            hideSuggestionWindow()
            return
        }
        val owner = SwingUtilities.getWindowAncestor(terminal) ?: return
        val window = suggestionWindow ?: JWindow(owner).also { created ->
            created.type = Window.Type.POPUP
            created.focusableWindowState = false
            created.isAutoRequestFocus = false
            created.background = Color(0, 0, 0, 0)
            created.rootPane.isOpaque = false
            (created.contentPane as? JComponent)?.isOpaque = false
            created.contentPane.layout = BorderLayout()
            created.contentPane.add(popup, BorderLayout.CENTER)
            suggestionWindow = created
        }
        popup.isVisible = true
        window.pack()
        positionSuggestionWindow()
        window.isVisible = true
    }

    private fun positionSuggestionWindow() {
        val window = suggestionWindow ?: return
        if ((!window.isVisible && suggestionPopup?.isVisible != true) || !terminal.isShowing) return
        val state = terminal.currentShellSuggestionState()
        if (!state.visible) return
        val configuration = terminal.graphicsConfiguration ?: return
        val screen = configuration.bounds
        val screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)
        val terminalOrigin = runCatching { terminal.locationOnScreen }.getOrNull() ?: return
        val fontMetrics = terminal.getFontMetrics(terminalSettings.font)
        val cellWidth = fontMetrics.charWidth('W').coerceAtLeast(1)
        val cellHeight = (fontMetrics.height * terminalSettings.lineHeight).toInt().coerceAtLeast(1)
        val anchorX = terminalOrigin.x + terminalSettings.padding.left + session.terminal.cursorCol * cellWidth
        val anchorTop = terminalOrigin.y + terminalSettings.padding.top + session.terminal.cursorRow * cellHeight
        val placement = SuggestionPopupPositioner.place(
            anchorX = anchorX,
            anchorTop = anchorTop,
            anchorBottom = anchorTop + cellHeight,
            popupWidth = window.width,
            popupHeight = window.height,
            viewportLeft = screen.x + screenInsets.left,
            viewportTop = screen.y + screenInsets.top,
            viewportRight = screen.x + screen.width - screenInsets.right,
            viewportBottom = screen.y + screen.height - screenInsets.bottom,
        )
        window.setLocation(placement.x, placement.y)
    }

    private fun hideSuggestionWindow() {
        suggestionWindow?.isVisible = false
    }

    private fun createSettings(): SwingSettings = SwingSettings(
        font = Font("Consolas", Font.PLAIN, 13),
        palette = gitCraftPalette(),
        cursorBlinkMillis = 550,
        padding = Insets(2, 8, 4, 4),
        alternateScreenPadding = Insets(2, 4, 2, 4),
        scrollbackLines = 5_000,
        lineHeight = 0.95f,
        shellSuggestionsEnabled = true,
    )

    private fun gitCraftPalette(): TerminalColorPalette {
        val base = TerminalTheme.TOKYO_NIGHT.createPalette()
        val indexed = IntArray(256)
        base.copyIndexedColorsInto(indexed)
        return TerminalColorPalette(
            defaultForeground = DarkTextPrimary.toArgb(),
            defaultBackground = DarkBackground.toArgb(),
            selectionForeground = LightTextPrimary.toArgb(),
            selectionBackground = DarkBorder.toArgb(),
            cursorForeground = DarkBackground.toArgb(),
            cursorBackground = GitCraftPurple.toArgb(),
            indexedColors = indexed,
        )
    }

    private companion object {
        const val AUTO_COMPLETE_DELAY_MILLIS = 120L
    }
}
