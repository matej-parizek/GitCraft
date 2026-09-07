package cz.parizmat.gitcraft

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.*
import cz.parizmat.gitcraft.core.di.appModule
import cz.parizmat.gitcraft.core.ui.theme.GitCraftTheme
import cz.parizmat.gitcraft.feature.main.MainScreen
import cz.parizmat.gitcraft.feature.toolbar.GitCraftToolBar
import cz.parizmat.gitcraft.resources.Res
import cz.parizmat.gitcraft.resources.gitcraft_logo
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(appModule)
    }
    val windowState = rememberWindowState()

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "GitCraft",
        icon = painterResource(Res.drawable.gitcraft_logo),
        undecorated = true,
    ) {
        GitCraftApp(
            onMinimize = {
                windowState.isMinimized = true
            },
            onToggleMaximize = {
                windowState.placement =
                    if (windowState.placement == WindowPlacement.Maximized) {
                        WindowPlacement.Floating
                    } else {
                        WindowPlacement.Maximized
                    }
            },
            onClose = ::exitApplication,
            isFloating = windowState.placement == WindowPlacement.Floating,
        )
    }
}

@Composable
fun FrameWindowScope.GitCraftApp(
    isFloating: Boolean,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit,
) {
    GitCraftTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            WindowDraggableArea {
                GitCraftToolBar(
                    projectName = "GitCraft",
                    branchName = "main",
                    buildConfig = "GitCraft [build]",
                    onMinimize = onMinimize,
                    onToggleMaximize = onToggleMaximize,
                    onClose = onClose,
                    isFloating = isFloating,
                )
            }

            MainScreen()
        }
    }
}