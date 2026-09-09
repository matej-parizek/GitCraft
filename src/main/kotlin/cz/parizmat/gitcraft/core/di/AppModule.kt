package cz.parizmat.gitcraft.core.di

import cz.parizmat.gitcraft.core.git.CliClient
import cz.parizmat.gitcraft.core.git.CommandExecutor
import cz.parizmat.gitcraft.core.git.GitClient
import cz.parizmat.gitcraft.feature.sidebar.ui.SidebarModelView
import cz.parizmat.gitcraft.core.domain.element.Repository
import cz.parizmat.gitcraft.core.terminal.KetraTerminalSessionFactory
import cz.parizmat.gitcraft.core.terminal.TerminalSessionFactory
import cz.parizmat.gitcraft.core.terminal.completion.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.dsl.module

val appModule = module(createdAtStart = true) {
    single {
        SidebarModelView()
    }
    single{
        CommandExecutor()
    }

    single<GitClient> {
        CliClient(get())
    }

    single<TerminalSessionFactory> { KetraTerminalSessionFactory() }
    single { CommandHistoryCompletionProvider() }
    single { RepositoryPathCompletionProvider() }
    single { PowerShellCompletionProvider() }
    single {
        val gitClient = get<GitClient>()
        GitCommandCompletionProvider { request ->
            withContext(Dispatchers.IO) {
                gitClient.references(Repository(request.repositoryRoot, null)).fold(
                    { GitReferences() },
                    { references -> GitReferences(references.localBranches, references.remoteBranches, references.tags) },
                )
            }
        }
    }
    single {
        TerminalCompletionService(
            listOf(
                get<GitCommandCompletionProvider>(),
                get<PowerShellCompletionProvider>(),
                get<RepositoryPathCompletionProvider>(),
                get<CommandHistoryCompletionProvider>(),
            ),
        )
    }

}
