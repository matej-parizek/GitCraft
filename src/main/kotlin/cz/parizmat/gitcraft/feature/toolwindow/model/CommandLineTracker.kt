package cz.parizmat.gitcraft.feature.toolwindow.model

import cz.parizmat.gitcraft.core.terminal.completion.CompletionCandidate
import cz.parizmat.gitcraft.core.terminal.completion.CompletionRequest
import cz.parizmat.gitcraft.core.terminal.completion.ReplacementMode
import java.nio.file.Path

class CommandLineTracker {
    private var value: String = ""

    val commandLine: String get() = value

    fun append(character: Char) {
        if (!character.isISOControl()) value += character
    }

    fun backspace() {
        if (value.isNotEmpty()) value = value.dropLast(1)
    }

    fun clear(): String = value.also { value = "" }

    fun accept(candidate: CompletionCandidate, repositoryRoot: Path): AcceptedCompletion {
        val request = CompletionRequest(value, value.length, repositoryRoot)
        val removedCharacters = when (candidate.replacementMode) {
            ReplacementMode.ACTIVE_TOKEN -> request.activeToken.length
            ReplacementMode.WHOLE_LINE -> value.length
        }
        value = when (candidate.replacementMode) {
            ReplacementMode.ACTIVE_TOKEN -> request.replaceActiveToken(candidate.text)
            ReplacementMode.WHOLE_LINE -> candidate.text
        }
        return AcceptedCompletion(removedCharacters, candidate.text)
    }
}

data class AcceptedCompletion(val removedCharacters: Int, val insertedText: String)
