package cz.parizmat.gitcraft.core.di

import cz.parizmat.gitcraft.core.git.CliClient
import cz.parizmat.gitcraft.core.git.CommandExecutor
import cz.parizmat.gitcraft.core.git.GitClient
import cz.parizmat.gitcraft.feature.sidebar.ui.SidebarModelView
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

}