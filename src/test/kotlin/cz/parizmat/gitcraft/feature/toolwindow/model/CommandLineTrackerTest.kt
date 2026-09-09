package cz.parizmat.gitcraft.feature.toolwindow.model

import cz.parizmat.gitcraft.core.terminal.completion.CompletionCandidate
import cz.parizmat.gitcraft.core.terminal.completion.CompletionType
import cz.parizmat.gitcraft.core.terminal.completion.ReplacementMode
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class CommandLineTrackerTest {
    @Test
    fun `active token completion only replaces the typed suffix`() {
        val tracker = CommandLineTracker().apply { "git sw".forEach(::append) }

        val accepted = tracker.accept(
            CompletionCandidate("switch", CompletionType.COMMAND, "Switch branches"),
            Path("."),
        )

        assertEquals(2, accepted.removedCharacters)
        assertEquals("switch", accepted.insertedText)
        assertEquals("git switch", tracker.commandLine)
    }

    @Test
    fun `history completion replaces the whole tracked line`() {
        val tracker = CommandLineTracker().apply { "git st".forEach(::append) }

        val accepted = tracker.accept(
            CompletionCandidate(
                text = "git status --short",
                type = CompletionType.HISTORY,
                description = "Command history",
                replacementMode = ReplacementMode.WHOLE_LINE,
            ),
            Path("."),
        )

        assertEquals(6, accepted.removedCharacters)
        assertEquals("git status --short", accepted.insertedText)
        assertEquals("git status --short", tracker.commandLine)
    }
}
