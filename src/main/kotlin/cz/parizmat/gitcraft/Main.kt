package cz.parizmat.gitcraft

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import cz.parizmat.gitcraft.core.git.GitClient
import cz.parizmat.gitcraft.feature.changes.ChangesModel
import cz.parizmat.gitcraft.core.terminal.TerminalSessionFactory
import cz.parizmat.gitcraft.core.terminal.completion.CommandHistoryCompletionProvider
import cz.parizmat.gitcraft.core.terminal.completion.TerminalCompletionService
import cz.parizmat.gitcraft.feature.toolwindow.viewmodel.TerminalToolWindowViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.compose.koinInject
import java.awt.Dimension
import javax.swing.JFileChooser
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.*
import cz.parizmat.gitcraft.core.di.appModule
import cz.parizmat.gitcraft.core.ui.theme.GitCraftTheme
import cz.parizmat.gitcraft.feature.main.MainScreen
import cz.parizmat.gitcraft.feature.history.viewmodel.HistoryViewModel
import cz.parizmat.gitcraft.feature.toolbar.GitCraftToolBar
import cz.parizmat.gitcraft.resources.Res
import cz.parizmat.gitcraft.resources.gitcraft_logo
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModule)
    }
    application {
    val windowState = rememberWindowState(width = 1280.dp, height = 840.dp)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "GitCraft",
        icon = painterResource(Res.drawable.gitcraft_logo),
        undecorated = true,
    ) {
        window.minimumSize = Dimension(1000, 700)
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
}

@Composable
fun FrameWindowScope.GitCraftApp(
    isFloating: Boolean,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit,
) {
    val git: GitClient = koinInject()
    val scope = rememberCoroutineScope()
    val model = remember(git, scope) { ChangesModel(git, scope) }
    val historyViewModel = remember(git, scope) { HistoryViewModel(git, scope) }
    val terminalFactory: TerminalSessionFactory = koinInject()
    val completionService: TerminalCompletionService = koinInject()
    val commandHistory: CommandHistoryCompletionProvider = koinInject()
    val terminalViewModel = remember(terminalFactory, completionService, commandHistory, scope) {
        TerminalToolWindowViewModel(terminalFactory, completionService, commandHistory)
    }
    val state by model.state.collectAsState()
    var openDialog by remember { mutableStateOf(false) }
    var repositoryPath by remember { mutableStateOf(System.getProperty("gitcraft.repository", System.getProperty("user.dir"))) }

    LaunchedEffect(model) {
        model.open(repositoryPath)
        while (isActive) { delay(3000); model.refresh() }
    }
    LaunchedEffect(state.repository) {
        terminalViewModel.setRepository(state.repository)
        historyViewModel.setRepository(state.repository)
    }
    DisposableEffect(terminalViewModel) { onDispose(terminalViewModel::close) }
    GitCraftTheme() {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            WindowDraggableArea {
                GitCraftToolBar(
                    projectName = state.repository?.rootPath?.fileName?.toString() ?: "No repository",
                    branchName = state.repository?.currentBranch ?: if (state.hasCommits) "Detached HEAD" else "",
                    onMinimize = onMinimize,
                    onToggleMaximize = onToggleMaximize,
                    onClose = onClose,
                    isFloating = isFloating,
                )
            }

            MainScreen(model, historyViewModel, terminalViewModel, onOpenRepository = { openDialog = true })
        }
        if (openDialog) AlertDialog(
            onDismissRequest = { openDialog = false },
            title = { Text("Open repository") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose an existing local Git repository.")
                    if (state.message.isNotBlank()) Text("Opening another repository clears the current commit message. Files and staged changes stay on disk.", color = MaterialTheme.colorScheme.error)
                    OutlinedTextField(value = repositoryPath, onValueChange = { repositoryPath = it }, label = { Text("Repository folder") }, singleLine = true)
                    TextButton(onClick = {
                        val chooser = JFileChooser(repositoryPath).apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY; dialogTitle = "Open Git repository" }
                        if (chooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) repositoryPath = chooser.selectedFile.absolutePath
                    }) { Text("Browse folders…") }
                }
            },
            dismissButton = { TextButton(onClick = { openDialog = false }) { Text("Cancel") } },
            confirmButton = { Button(onClick = { openDialog = false; model.open(repositoryPath) }, enabled = repositoryPath.isNotBlank() && !state.busy) { Text("Open repository") } },
        )
        }
    }
}
