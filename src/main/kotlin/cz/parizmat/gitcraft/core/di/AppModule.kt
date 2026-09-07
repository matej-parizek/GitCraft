package cz.parizmat.gitcraft.core.di

import cz.parizmat.gitcraft.core.git.CliClient
import cz.parizmat.gitcraft.core.git.CommandExecutor
import cz.parizmat.gitcraft.core.git.GitClient
import org.koin.dsl.module

val appModule = module(createdAtStart = true) {
    single{
        CommandExecutor()
    }

    single<GitClient> {
        CliClient(get())
    }

}