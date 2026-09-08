package cz.parizmat.gitcraft.feature.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChangesScreen(model: ChangesModel, onOpenRepository: () -> Unit) {
    val state by model.state.collectAsState()
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmAmend by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        ChangesToolbar(state, model::stageAll, model::unstageAll, { confirmDiscard = true }, model::refresh)
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
        else Spacer(Modifier.height(2.dp))
        val feedback = state.error ?: state.notice
        if (feedback != null) {
            Row(
                Modifier.fillMaxWidth().background(
                    if (state.error != null) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.1f
                    )
                ).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    feedback,
                    Modifier.weight(1f).padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = model::dismissFeedback) { Text("Dismiss") }
            }
        }
        if (state.repository == null) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.FolderOpen, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Your next commit starts here", style = MaterialTheme.typography.titleLarge)
                GuidedInfo("Open a local Git repository to review and commit your changes.")
                Spacer(Modifier.height(20.dp))
                Button(onClick = onOpenRepository, enabled = !state.busy) { Text("Open repository") }
            }
        } else {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                FileChangesPanel(state, model, Modifier.width(300.dp).fillMaxHeight())
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                DiffViewer(state, Modifier.weight(1f).fillMaxHeight())
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            CommitPanel(state, model::setMessage, model::setAmend) {
                if (state.amend) confirmAmend = true else model.commit()
            }
        }
    }
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Discard all changes?") },
        text = { Text("This permanently removes all staged and unstaged changes, including untracked files, from this repository. Ignored files are kept.\n\nThis action cannot be undone using GitCraft.") },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Cancel") } },
        confirmButton = {
            Button(
                onClick = { confirmDiscard = false; model.discardAll() },
                enabled = !state.busy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Discard changes") }
        },
    )
    if (confirmAmend) AlertDialog(
        onDismissRequest = { confirmAmend = false },
        title = { Text("Replace the latest commit?") },
        text = { Text("The staged changes and this message will replace the latest commit, creating a new commit ID. If you already shared that commit, other people may depend on it. Nothing will be pushed.") },
        dismissButton = { TextButton(onClick = { confirmAmend = false }) { Text("Cancel") } },
        confirmButton = {
            Button(
                onClick = { confirmAmend = false; model.commit() },
                enabled = state.canCommit,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Replace commit") }
        },
    )
}

@Composable
private fun ChangesToolbar(
    state: ChangesUiState,
    onStageAll: () -> Unit,
    onUnstageAll: () -> Unit,
    onDiscard: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text("Changes", style = MaterialTheme.typography.headlineMedium)
            GuidedInfo("Review your changes, stage files and create a commit.")
        }
        OutlinedButton(
            onClick = onStageAll,
            enabled = !state.busy && state.unstaged.isNotEmpty() && !state.conflicted
        ) { Text("Stage all") }
        OutlinedButton(
            onClick = onUnstageAll,
            enabled = !state.busy && state.staged.isNotEmpty() && !state.conflicted
        ) { Text("Unstage all") }
        TextButton(
            onClick = onDiscard,
            enabled = !state.busy && state.changes.isNotEmpty() && !state.conflicted,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text("Discard all") }
        IconButton(
            onClick = onRefresh,
            enabled = !state.busy && state.repository != null
        ) { Icon(Icons.Outlined.Refresh, "Refresh changes") }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

@Composable
fun GuidedInfo(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
