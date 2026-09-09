package cz.parizmat.gitcraft.feature.toolwindow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.parizmat.gitcraft.feature.toolwindow.viewmodel.TerminalToolWindowViewModel
import java.awt.Cursor

@Composable
fun TerminalToolWindow(
    viewModel: TerminalToolWindowViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        if (state.isExpanded) {
            Box(
                Modifier.fillMaxWidth().height(4.dp)
                    .background(MaterialTheme.colorScheme.outline)
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)))
                    .pointerInput(viewModel) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            viewModel.resizeBy(dragAmount)
                        }
                    },
            )
        }
        TerminalHeader(
            repositoryName = state.repositoryRoot?.fileName?.toString(),
            expanded = state.isExpanded,
            canRestart = state.repositoryRoot != null,
            onRestart = viewModel::restart,
            onToggle = viewModel::toggleExpanded,
        )
        if (state.isExpanded) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Box(Modifier.fillMaxWidth().height(state.height.dp).background(MaterialTheme.colorScheme.background)) {
                when {
                    state.error != null -> TerminalMessage("Terminal could not start", state.error!!)
                    state.repositoryRoot == null -> TerminalMessage("No repository open", "Open a repository to start PowerShell in its root folder.")
                    state.session != null -> key(state.session) {
                        val hostScope = rememberCoroutineScope()
                        val host = remember(state.session, state.repositoryRoot) {
                            TerminalSwingHost(
                                session = state.session!!,
                                repositoryRoot = state.repositoryRoot!!,
                                completionService = viewModel.completionService,
                                history = viewModel.commandHistory,
                                scope = hostScope,
                            )
                        }
                        DisposableEffect(host) { onDispose(host::close) }
                        SwingPanel(
                            factory = host::createComponent,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalHeader(
    repositoryName: String?,
    expanded: Boolean,
    canRestart: Boolean,
    onRestart: () -> Unit,
    onToggle: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Terminal, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text("Terminal", style = MaterialTheme.typography.labelLarge)
        if (repositoryName != null) {
            Spacer(Modifier.width(12.dp))
            Text(repositoryName, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else Spacer(Modifier.weight(1f))
        IconButton(onClick = onRestart, enabled = canRestart, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Refresh, "Restart PowerShell", Modifier.size(17.dp))
        }
        IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
            Icon(if (expanded) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp, if (expanded) "Collapse terminal" else "Open terminal", Modifier.size(19.dp))
        }
    }
}

@Composable
private fun TerminalMessage(title: String, detail: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
