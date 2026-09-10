package cz.parizmat.gitcraft.feature.history.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import cz.parizmat.gitcraft.core.domain.element.Commit
import cz.parizmat.gitcraft.core.domain.element.DiffRow
import cz.parizmat.gitcraft.core.domain.element.DiffRowType
import cz.parizmat.gitcraft.core.domain.element.FileDiff
import cz.parizmat.gitcraft.core.ui.theme.DiffAddedBackground
import cz.parizmat.gitcraft.core.ui.theme.DiffDeletedBackground
import cz.parizmat.gitcraft.core.ui.theme.GitAdded
import cz.parizmat.gitcraft.core.ui.theme.GitDeleted
import cz.parizmat.gitcraft.core.ui.theme.HistoryDimensions
import cz.parizmat.gitcraft.feature.changes.GuidedInfo

@Composable
fun CommitDiffView(
    commit: Commit?,
    diff: FileDiff?,
    loading: Boolean,
    onBack: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    onBack()
                    true
                } else {
                    false
                }
            },
    ) {
        Row(
            Modifier.fillMaxWidth().height(HistoryDimensions.diffToolbarHeight)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = HistoryDimensions.spacingMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HistoryDimensions.spacingSm),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to history")
            }
            Text(
                text = commit?.shortHash.orEmpty(),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
            )
            Text(
                text = commit?.message.orEmpty(),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onBack) { Text("Back to history") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        DiffColumnHeader()
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            diff == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GuidedInfo("The commit diff is unavailable.") }
            diff.isBinary -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GuidedInfo("This commit contains binary changes that cannot be previewed.") }
            diff.rows.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GuidedInfo("This commit has no textual changes.") }
            else -> DiffRows(diff)
        }
    }
}

@Composable
private fun DiffColumnHeader() {
    Row(
        Modifier.fillMaxWidth().height(HistoryDimensions.tableHeaderHeight)
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Old", Modifier.width(HistoryDimensions.diffLineNumberWidth), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("New", Modifier.width(HistoryDimensions.diffLineNumberWidth), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        VerticalDivider(color = MaterialTheme.colorScheme.outline)
        Text("Changes", Modifier.padding(start = HistoryDimensions.spacingMd), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DiffRows(diff: FileDiff) {
    val horizontalScroll = rememberScrollState()
    LazyColumn(Modifier.fillMaxSize()) {
        items(diff.rows) { row -> DiffRowView(row, horizontalScroll) }
        if (diff.isTruncated) item { GuidedInfo("Diff preview was truncated for performance.") }
    }
}

@Composable
private fun DiffRowView(row: DiffRow, horizontalScroll: androidx.compose.foundation.ScrollState) {
    val background = when (row.type) {
        DiffRowType.ADDED -> DiffAddedBackground
        DiffRowType.DELETED -> DiffDeletedBackground
        DiffRowType.HEADER -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    val foreground = when (row.type) {
        DiffRowType.ADDED -> GitAdded
        DiffRowType.DELETED -> GitDeleted
        DiffRowType.HEADER -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = HistoryDimensions.diffRowMinHeight).background(background),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineNumber(row.oldLine)
        LineNumber(row.newLine)
        VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
        Text(
            text = when (row.type) {
                DiffRowType.ADDED -> "+"
                DiffRowType.DELETED -> "−"
                else -> " "
            },
            modifier = Modifier.width(HistoryDimensions.diffMarkerWidth),
            color = foreground,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        )
        Text(
            text = row.text,
            modifier = Modifier.weight(1f).horizontalScroll(horizontalScroll).padding(end = HistoryDimensions.spacingMd),
            color = foreground,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            softWrap = false,
        )
    }
}

@Composable
private fun LineNumber(value: Int?) {
    Text(
        text = value?.toString().orEmpty(),
        modifier = Modifier.width(HistoryDimensions.diffLineNumberWidth).padding(end = HistoryDimensions.spacingSm),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
    )
}
