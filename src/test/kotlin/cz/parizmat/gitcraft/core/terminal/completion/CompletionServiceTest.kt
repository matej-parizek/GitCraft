package cz.parizmat.gitcraft.core.terminal.completion

import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompletionServiceTest {
    @Test
    fun `git command prefix ranks switch show and status`() = runBlocking {
        val service = TerminalCompletionService(listOf(GitCommandCompletionProvider { GitReferences() }))

        val result = service.complete(CompletionRequest("git sw", 6, Path.of(".")))

        assertEquals("switch", result.first().text)
        assertTrue(result.any { it.text == "show" && it.description.isNotBlank() })
        assertTrue(result.any { it.text == "status" && it.type == CompletionType.COMMAND })
    }

    @Test
    fun `git switch completes local branches without remote branches or tags`() = runBlocking {
        val provider = GitCommandCompletionProvider {
            GitReferences(localBranches = listOf("feature/terminal"), remoteBranches = listOf("origin/main"), tags = listOf("v1.0"))
        }

        val result = provider.complete(CompletionRequest("git switch fea", 14, Path.of(".")))

        assertEquals(listOf("feature/terminal"), result.map { it.text })
        assertEquals(CompletionType.BRANCH, result.single().type)
    }

    @Test
    fun `active token replacement preserves the rest of the command`() {
        val request = CompletionRequest("git sw --quiet", 6, Path.of("."))

        assertEquals("git switch --quiet", request.replaceActiveToken("switch"))
    }

    @Test
    fun `powershell provider delegates to native tab expansion`() = runBlocking {
        if (!System.getProperty("os.name").startsWith("Windows")) return@runBlocking

        val result = PowerShellCompletionProvider().complete(
            CompletionRequest("Get-Chi", 7, Path.of(".").toAbsolutePath()),
        )

        assertTrue(result.any { it.text.equals("Get-ChildItem", ignoreCase = true) }, result.toString())
    }
}
