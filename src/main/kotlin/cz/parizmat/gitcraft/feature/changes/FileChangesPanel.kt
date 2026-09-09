package cz.parizmat.gitcraft.feature.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.parizmat.gitcraft.core.domain.element.GitChange
import cz.parizmat.gitcraft.core.domain.element.enums.FileStatus
import cz.parizmat.gitcraft.core.ui.theme.*

@Composable
fun FileChangesPanel(state: ChangesUiState, model: ChangesModel, modifier: Modifier = Modifier) {
    Column(modifier.background(MaterialTheme.colorScheme.surface)) {
        if (state.changes.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(32.dp), tint = GitAdded)
                Spacer(Modifier.height(12.dp))
                Text("Working tree clean", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                GuidedInfo("There are no uncommitted changes.")
            }
        } else {
            if (state.conflicted) Text(
                "Resolve merge conflicts outside GitCraft before staging or committing.",
                Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            ChangedFilesSection(
                "Unstaged changes",
                "Stage files to include them in your commit.",
                state.unstaged,
                false,
                state,
                model,
                Modifier.weight(1f)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ChangedFilesSection(
                "Staged changes",
                "These files will be in your next commit.",
                state.staged,
                true,
                state,
                model,
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ChangedFilesSection(
    title: String,
    guidance: String,
    files: List<GitChange>,
    staged: Boolean,
    state: ChangesUiState,
    model: ChangesModel,
    modifier: Modifier
) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(
                files.size.toString(),
                color = if (staged) GitAdded else MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
        }
        GuidedInfo(guidance, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
        if (files.isEmpty()) GuidedInfo(if (staged) "No staged files" else "No unstaged files", Modifier.padding(16.dp))
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(files, key = { it.path.toString() }) { file ->
                ChangedFileRow(
                    file,
                    staged,
                    state.selection?.let { it.change.path == file.path && it.staged == staged } == true,
                    !state.busy && !state.conflicted,
                    { model.select(file, staged) },
                    { if (staged) model.unstage(file) else model.stage(file) })
            }
        }
    }
}

@Composable
private fun ChangedFileRow(
    file: GitChange,
    staged: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    onAction: () -> Unit
) {
    val status = if (staged) file.indexStatus else file.workingTreeStatus
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent)
            .selectable(selected, onClick = onSelect, role = Role.Tab).padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            status.letter(),
            Modifier.width(24.dp),
            color = status.color(),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelLarge
        )
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                file.path.fileName.toString(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            val detail = file.oldPath?.let { "$it → ${file.path}" } ?: file.path.parent?.toString()
            if (detail != null) Text(
                detail,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(
            onClick = onAction,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) { Text(if (staged) "Unstage" else "Stage", style = MaterialTheme.typography.bodySmall) }
    }
}

internal fun FileStatus.letter(): String = when (this) {
    FileStatus.ADDED -> "A"
    FileStatus.MODIFIED -> "M"
    FileStatus.DELETED -> "D"
    FileStatus.RENAMED -> "R"
    FileStatus.COPIED -> "C"
    FileStatus.UNTRACKED -> "?"
    FileStatus.CONFLICTED -> "!"
    else -> "·"
}

internal fun FileStatus.color(): Color = when (this) {
    FileStatus.ADDED, FileStatus.UNTRACKED -> GitAdded
    FileStatus.DELETED, FileStatus.CONFLICTED -> GitDeleted
    else -> GitModified
}
