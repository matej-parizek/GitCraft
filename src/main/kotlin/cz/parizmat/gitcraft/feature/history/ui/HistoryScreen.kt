package cz.parizmat.gitcraft.feature.history.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import cz.parizmat.gitcraft.core.domain.element.Branch
import cz.parizmat.gitcraft.core.domain.element.Commit
import cz.parizmat.gitcraft.core.domain.element.CommitDetails
import cz.parizmat.gitcraft.core.domain.element.enums.BranchType
import cz.parizmat.gitcraft.core.ui.theme.HistoryDimensions
import cz.parizmat.gitcraft.feature.changes.GuidedInfo
import cz.parizmat.gitcraft.feature.history.model.CommitGraphRow
import cz.parizmat.gitcraft.feature.history.model.HistoryRef
import cz.parizmat.gitcraft.feature.history.model.HistoryRefType
import cz.parizmat.gitcraft.feature.history.model.HistoryUiState
import cz.parizmat.gitcraft.feature.history.viewmodel.HistoryViewModel
import java.time.format.DateTimeFormatter
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onOpenRepository: () -> Unit) {
    val state by viewModel.state.collectAsState()
    if (state.repository == null) {
        EmptyHistory(onOpenRepository)
        return
    }
    if (state.diffOpen) {
        CommitDiffView(
            commit = state.details?.commit,
            diff = state.diff,
            loading = state.diffLoading,
            onBack = viewModel::closeDiff,
        )
        return
    }

    val searchFocus = remember { FocusRequester() }
    var searchFocused by remember { mutableStateOf(false) }
    var revealSelectionRequest by remember { mutableIntStateOf(0) }
    Column(
        Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when {
                event.isCtrlPressed && event.key == Key.F -> { searchFocus.requestFocus(); true }
                !searchFocused && event.key == Key.DirectionUp -> { viewModel.moveSelection(-1); true }
                !searchFocused && event.key == Key.DirectionDown -> { viewModel.moveSelection(1); true }
                !searchFocused && event.key == Key.Enter -> { revealSelectionRequest++; true }
                !searchFocused && event.key == Key.R -> { viewModel.refresh(); true }
                else -> false
            }
        },
    ) {
        HistoryToolbar(state, viewModel::setQuery, viewModel::refresh, searchFocus) { searchFocused = it }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().height(HistoryDimensions.progressHeight)) else Spacer(Modifier.height(HistoryDimensions.progressHeight))
        state.error?.let { error ->
            Text(
                error,
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)).padding(horizontal = HistoryDimensions.spacingLg, vertical = HistoryDimensions.spacingSm),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val showBranches = maxWidth >= HistoryDimensions.showBranchesMinWidth
            val showDetails = maxWidth >= HistoryDimensions.showDetailsMinWidth
            Row(Modifier.fillMaxSize()) {
                if (showBranches) {
                    BranchFilterPanel(state, viewModel::selectRef, Modifier.width(HistoryDimensions.branchPanelWidth).fillMaxHeight())
                    VerticalDivider(color = MaterialTheme.colorScheme.outline)
                }
                CommitHistoryPane(state, revealSelectionRequest, viewModel::selectCommit, Modifier.weight(1f).fillMaxHeight())
                if (showDetails) {
                    VerticalDivider(color = MaterialTheme.colorScheme.outline)
                    CommitDetailsPanel(state.details, state.detailsLoading, viewModel::openDiff, Modifier.width(HistoryDimensions.detailsPanelWidth).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun HistoryToolbar(
    state: HistoryUiState,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    searchFocus: FocusRequester,
    onSearchFocusChanged: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(HistoryDimensions.toolbarHeight).padding(horizontal = HistoryDimensions.spacingXl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HistoryDimensions.spacingMd),
    ) {
        Column(Modifier.weight(1f)) {
            Text("History", style = MaterialTheme.typography.headlineMedium)
            GuidedInfo("Explore branches, merges and commits in this repository.")
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.width(HistoryDimensions.searchWidth).focusRequester(searchFocus).onFocusChanged { onSearchFocusChanged(it.isFocused) },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            placeholder = { Text("Search commits") },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        IconButton(onClick = onRefresh, enabled = !state.loading) {
            Icon(Icons.Outlined.Refresh, "Refresh history")
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun BranchFilterPanel(state: HistoryUiState, onSelect: (HistoryRef?) -> Unit, modifier: Modifier = Modifier) {
    val tags = remember(state.rows) { state.rows.flatMap { it.commit.tags }.distinct().sorted() }
    LazyColumn(modifier.background(MaterialTheme.colorScheme.surface).padding(HistoryDimensions.spacingMd)) {
        item { FilterRow("All history", Icons.Outlined.AllInclusive, state.selectedRef == null) { onSelect(null) } }
        branchGroup("LOCAL", state.branches.filter { it.type == BranchType.LOCAL }, state.selectedRef, onSelect)
        branchGroup("REMOTE", state.branches.filter { it.type == BranchType.REMOTE }, state.selectedRef, onSelect)
        if (tags.isNotEmpty()) {
            item { GroupLabel("TAGS") }
            items(tags, key = { "tag:$it" }) { tag ->
                val ref = HistoryRef(HistoryRefType.TAG, tag)
                FilterRow(tag, Icons.Outlined.Tag, state.selectedRef == ref) { onSelect(ref) }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.branchGroup(
    title: String,
    branches: List<Branch>,
    selectedRef: HistoryRef?,
    onSelect: (HistoryRef?) -> Unit,
) {
    if (branches.isEmpty()) return
    item { GroupLabel(title) }
    items(branches, key = { "branch:${it.name}" }) { branch ->
        val ref = HistoryRef(HistoryRefType.BRANCH, branch.name)
        FilterRow(
            label = branch.name,
            icon = if (branch.type == BranchType.REMOTE) Icons.Outlined.Cloud else Icons.Outlined.AccountTree,
            selected = selectedRef == ref,
            supportingText = when {
                branch.isCurrent -> "Current"
                branch.ahead > 0 || branch.behind > 0 -> "↑${branch.ahead} ↓${branch.behind}"
                else -> null
            },
        ) { onSelect(ref) }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        Modifier.padding(start = HistoryDimensions.spacingSm, top = HistoryDimensions.spacingXl, bottom = HistoryDimensions.spacingSm),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FilterRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    supportingText: String? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val background = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        hovered -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(HistoryDimensions.rowRadius)).background(background)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { this.selected = selected }.padding(horizontal = HistoryDimensions.spacingSm, vertical = HistoryDimensions.spacingSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(HistoryDimensions.iconSize), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(HistoryDimensions.spacingSm))
        Text(label, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        supportingText?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun CommitHistoryPane(state: HistoryUiState, revealSelectionRequest: Int, onSelect: (Commit) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val showAuthor = maxWidth >= HistoryDimensions.showAuthorMinWidth
        val showDate = maxWidth >= HistoryDimensions.showDateMinWidth
        val listState = rememberLazyListState()
        LaunchedEffect(state.selectedHash, revealSelectionRequest, state.visibleRows) {
            val selectedIndex = state.visibleRows.indexOfFirst { it.commit.hash == state.selectedHash }
            if (selectedIndex < 0) return@LaunchedEffect
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.none { it.index == selectedIndex }) listState.scrollToItem(selectedIndex)
        }

        Column(Modifier.fillMaxSize().focusable()) {
            Row(
                Modifier.fillMaxWidth().height(HistoryDimensions.tableHeaderHeight).background(MaterialTheme.colorScheme.surface).padding(horizontal = HistoryDimensions.spacingMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Graph", Modifier.width(HistoryDimensions.graphColumnWidth), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Commit", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (showAuthor) {
                    Text("Author", Modifier.width(HistoryDimensions.authorColumnWidth), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (showDate) {
                    Text("Date", Modifier.width(HistoryDimensions.dateColumnWidth), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            when {
                state.loading && state.rows.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.visibleRows.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GuidedInfo(if (state.query.isNotBlank() || state.selectedRef != null) "No commits match this filter." else "This repository has no commits yet.")
                }
                else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                    items(state.visibleRows, key = { it.commit.hash }, contentType = { "commit" }) { row ->
                        CommitRow(
                            row = row,
                            selected = row.commit.hash == state.selectedHash,
                            showAuthor = showAuthor,
                            showDate = showDate,
                            onClick = { onSelect(row.commit) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommitRow(
    row: CommitGraphRow,
    selected: Boolean,
    showAuthor: Boolean,
    showDate: Boolean,
    onClick: () -> Unit,
) {
    val commit = row.commit
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    Row(
        Modifier.fillMaxWidth().height(HistoryDimensions.commitRowHeight)
            .background(if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f) else Color.Transparent)
            .drawBehind {
                val strokeWidth = HistoryDimensions.dividerThickness.toPx()
                val y = size.height - strokeWidth / 2f
                drawLine(dividerColor, Offset(0f, y), Offset(size.width, y), strokeWidth)
            }
            .clickable(onClick = onClick).semantics { this.selected = selected }.padding(horizontal = HistoryDimensions.spacingMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CommitGraph(row, Modifier.width(HistoryDimensions.graphColumnWidth).fillMaxHeight())
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(commit.message, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            Row(horizontalArrangement = Arrangement.spacedBy(HistoryDimensions.spacingSm), verticalAlignment = Alignment.CenterVertically) {
                Text(commit.shortHash, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
                commit.branch.take(2).forEach { RefLabel(it.name, MaterialTheme.colorScheme.primary) }
                commit.tags.take(1).forEach { RefLabel(it, MaterialTheme.colorScheme.tertiary) }
            }
        }
        if (showAuthor) {
            Text(commit.authorName, Modifier.width(HistoryDimensions.authorColumnWidth), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        }
        if (showDate) {
            Text(COMMIT_DATE_FORMAT.format(commit.date), Modifier.width(HistoryDimensions.dateColumnWidth), maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RefLabel(text: String, color: Color) {
    Text(
        text,
        modifier = Modifier.clip(RoundedCornerShape(HistoryDimensions.rowRadius)).background(color.copy(alpha = 0.14f)).padding(horizontal = HistoryDimensions.spacingXs),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
    )
}

@Composable
private fun CommitDetailsPanel(details: CommitDetails?, loading: Boolean, onOpenDiff: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.background(MaterialTheme.colorScheme.surface).padding(HistoryDimensions.spacingLg)) {
        Text("Commit details", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(HistoryDimensions.spacingLg))
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            details == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GuidedInfo("Select a commit to inspect it.") }
            else -> {
                val commit = details.commit
                Text(commit.message, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(HistoryDimensions.spacingSm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(commit.shortHash, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(HistoryDimensions.spacingXs))
                    TextButton(onClick = { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(commit.hash), null) }) { Text("Copy hash") }
                }
                DetailLine("Author", "${commit.authorName} <${commit.authorEmail}>")
                DetailLine("Date", DETAIL_DATE_FORMAT.format(commit.date))
                DetailLine("Parents", commit.parentsHashes.joinToString { it.take(7) }.ifBlank { "Root commit" })
                Spacer(Modifier.height(HistoryDimensions.spacingMd))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(HistoryDimensions.spacingMd))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${details.files.size} files changed", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    Text("+${details.additions}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(HistoryDimensions.spacingSm))
                    Text("−${details.deletions}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(HistoryDimensions.spacingSm))
                LazyColumn(Modifier.weight(1f)) {
                    items(details.files, key = { it.path.toString() }) { file ->
                        Row(Modifier.fillMaxWidth().padding(vertical = HistoryDimensions.spacingSm), verticalAlignment = Alignment.CenterVertically) {
                            Text(file.path.fileName.toString(), Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (file.additions == null || file.deletions == null) "binary" else "+${file.additions} −${file.deletions}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (commit.body.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Text(commit.body, Modifier.padding(top = HistoryDimensions.spacingMd), maxLines = 5, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(HistoryDimensions.spacingMd))
                Button(onClick = onOpenDiff, enabled = details.files.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Text("Open diff")
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = HistoryDimensions.spacingXs)) {
        Text(label, Modifier.width(HistoryDimensions.detailLabelWidth), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyHistory(onOpenRepository: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.FolderOpen, null, Modifier.size(HistoryDimensions.emptyIconSize), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(HistoryDimensions.spacingLg))
        Text("Open a repository to explore its history", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(HistoryDimensions.spacingLg))
        Button(onClick = onOpenRepository) { Text("Open repository") }
    }
}

private val COMMIT_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")
private val DETAIL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm")
