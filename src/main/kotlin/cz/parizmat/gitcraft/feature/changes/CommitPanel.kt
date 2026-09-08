package cz.parizmat.gitcraft.feature.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CommitPanel(state: ChangesUiState, onMessage: (String) -> Unit, onAmend: (Boolean) -> Unit, onCommit: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Commit message", style = MaterialTheme.typography.titleMedium)
            GuidedInfo("A local snapshot of your staged changes.")
        }
        OutlinedTextField(
            value = state.message, onValueChange = onMessage, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().height(88.dp),
            placeholder = { Text("Describe what changed and why…", style = MaterialTheme.typography.bodyMedium) },
            textStyle = MaterialTheme.typography.bodyMedium,
            label = { Text("Commit message") },
        )
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(checked = state.amend, onCheckedChange = onAmend, enabled = state.hasCommits && !state.busy)
            Column(Modifier.weight(1f)) {
                Text("Amend last commit", style = MaterialTheme.typography.bodyMedium)
                GuidedInfo(if (state.hasCommits) "Replace the latest commit with this one." else "Available after your first commit.")
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (state.staged.isEmpty()) "No staged changes" else "${state.staged.size} ${if (state.staged.size == 1) "file" else "files"} staged",
                    style = MaterialTheme.typography.bodyMedium
                )
                GuidedInfo(if (state.staged.isEmpty()) "Stage at least one file to commit." else "Included in your next commit.")
            }
            Button(onClick = onCommit, enabled = state.canCommit, shape = MaterialTheme.shapes.small) {
                Text(if (state.busy) "Working…" else "Commit changes")
            }
        }
    }
}
