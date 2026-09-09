package cz.parizmat.gitcraft.core.terminal.completion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.charset.StandardCharsets
import java.util.Base64

class PowerShellCompletionProvider : CompletionProvider {
    override suspend fun complete(request: CompletionRequest): List<CompletionCandidate> = withContext(Dispatchers.IO) {
        if (request.commandLine.isBlank()) return@withContext emptyList()
        val process = ProcessBuilder(POWERSHELL, NO_LOGO, NO_PROFILE, NON_INTERACTIVE, COMMAND, COMPLETION_SCRIPT)
            .redirectErrorStream(true)
            .apply {
                environment()[LINE_ENVIRONMENT] = request.commandLine
                environment()[CURSOR_ENVIRONMENT] = request.cursorOffset.toString()
                directory(request.repositoryRoot.toFile())
            }.start()
        try {
            coroutineScope {
                val output = async { process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() } }
                val completed = withTimeoutOrNull(TIMEOUT_MILLIS) {
                    while (process.isAlive) delay(PROCESS_POLL_MILLIS)
                    true
                } ?: false
                if (!completed) {
                    process.destroyForcibly()
                    return@coroutineScope emptyList()
                }
                output.await().lineSequence().mapNotNull(::parseLine).take(MAX_RESULTS).toList()
            }
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }

    private fun parseLine(line: String): CompletionCandidate? {
        val fields = line.split('\t')
        if (fields.size != FIELD_COUNT) return null
        return runCatching {
            val text = decode(fields[0])
            if (text.isEmpty()) return null
            val resultType = decode(fields[1])
            CompletionCandidate(
                text = text,
                type = if (resultType.contains("Command", ignoreCase = true)) CompletionType.COMMAND else CompletionType.POWERSHELL,
                description = decode(fields[2]).ifBlank { "PowerShell completion" },
                score = POWERSHELL_SCORE,
            )
        }.getOrNull()
    }

    private fun decode(value: String): String = String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8)

    private companion object {
        const val POWERSHELL = "powershell.exe"
        const val NO_LOGO = "-NoLogo"
        const val NO_PROFILE = "-NoProfile"
        const val NON_INTERACTIVE = "-NonInteractive"
        const val COMMAND = "-Command"
        const val LINE_ENVIRONMENT = "GITCRAFT_COMPLETION_LINE"
        const val CURSOR_ENVIRONMENT = "GITCRAFT_COMPLETION_CURSOR"
        const val TIMEOUT_MILLIS = 900L
        const val PROCESS_POLL_MILLIS = 20L
        const val MAX_RESULTS = 12
        const val FIELD_COUNT = 3
        const val POWERSHELL_SCORE = 65
        val COMPLETION_SCRIPT = """
            ${'$'}ErrorActionPreference = 'Stop'
            ${'$'}result = TabExpansion2 -inputScript ${'$'}env:$LINE_ENVIRONMENT -cursorColumn ([int]${'$'}env:$CURSOR_ENVIRONMENT)
            ${'$'}result.CompletionMatches | Select-Object -First $MAX_RESULTS | ForEach-Object {
                ${'$'}encode = { param([string]${'$'}value) [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(${'$'}value)) }
                (& ${'$'}encode ${'$'}_.CompletionText) + [char]9 + (& ${'$'}encode ${'$'}_.ResultType.ToString()) + [char]9 + (& ${'$'}encode ${'$'}_.ToolTip)
            }
        """.trimIndent()
    }
}
