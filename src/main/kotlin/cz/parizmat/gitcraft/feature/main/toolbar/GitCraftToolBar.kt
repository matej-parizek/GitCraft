package cz.parizmat.gitcraft.feature.main.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import cz.parizmat.gitcraft.core.ui.components.Logo
import io.github.lyxnx.compose.ui.tablericons.TablerIcons
import io.github.lyxnx.compose.ui.tablericons.filled.*
import io.github.lyxnx.compose.ui.tablericons.outline.Search

@Composable
fun GitCraftToolBar(
    projectName: String,
    branchName: String,
    buildConfig: String,
    isFloating: Boolean = false,
    onProjectClick: () -> Unit = {},
    onBranchClick: () -> Unit = {},
    onBuildConfigClick: () -> Unit = {},
    onRun: () -> Unit = {},
    onDebug: () -> Unit = {},
    onMoreActions: () -> Unit = {},
    onAiAssistant: () -> Unit = {},
    onSearchEverywhere: () -> Unit = {},
    onSettings: () -> Unit = {},
    onStartTrial: () -> Unit = {},
    onMinimize: () -> Unit = {},
    onToggleMaximize: () -> Unit = {},
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ),
                )
                .height(40.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Logo(
                modifier = Modifier.size(16.dp),
            )
//
//            ProjectChip(
//                label = projectName,
//                colors = colors,
//                onClick = onProjectClick,
//            )
//
//            Spacer(
//                modifier = Modifier.width(6.dp),
//            )
//
//            DropdownChip(
//                icon = TablerIcons.Filled.BrandGithub,
//                label = branchName,
//                colors = colors,
//                onClick = onBranchClick,
//            )
//
//            Spacer(
//                modifier = Modifier.weight(1f),
//            )
//
//            DropdownChip(
//                icon = TablerIcons.Filled.LayoutGrid,
//                label = buildConfig,
//                colors = colors,
//                onClick = onBuildConfigClick,
//            )
//
//            Spacer(
//                modifier = Modifier.width(6.dp),
//            )
//
//            IconGhostButton(
//                icon = TablerIcons.Filled.PlayerPlay,
//                contentDescription = "Run",
//                colors = colors,
//                tint = colors.accentGreen,
//                onClick = onRun,
//            )
//
//            IconGhostButton(
//                icon = TablerIcons.Filled.Bug,
//                contentDescription = "Debug",
//                colors = colors,
//                onClick = onDebug,
//            )
//
//            IconGhostButton(
//                icon = TablerIcons.Filled.DotsVertical,
//                contentDescription = "More actions",
//                colors = colors,
//                onClick = onMoreActions,
//            )
//
//            Spacer(
//                modifier = Modifier.width(12.dp),
//            )
//
//            ToolbarDivider(
//                colors = colors,
//            )
//
//            Spacer(
//                modifier = Modifier.width(8.dp),
//            )
//
//            IconGhostButton(
//                icon = TablerIcons.Filled.XboxA,
//                contentDescription = "AI assistant",
//                colors = colors,
//                onClick = onAiAssistant,
//            )
//
//            IconGhostButton(
//                icon = TablerIcons.Outline.Search,
//                contentDescription = "Search",
//                colors = colors,
//                onClick = onSearchEverywhere,
//            )
//
//            IconGhostButton(
//                icon = TablerIcons.Filled.Settings,
//                contentDescription = "Settings",
//                colors = colors,
//                showBadge = true,
//                onClick = onSettings,
//            )
//
//            Spacer(
//                modifier = Modifier.width(8.dp),
//            )
//
//            StartTrialButton(
//                colors = colors,
//                onClick = onStartTrial,
//            )
//
//            Spacer(
//                modifier = Modifier.width(10.dp),
//            )
//
//            ToolbarDivider(
//                colors = colors,
//            )

            Spacer(
                modifier = Modifier.weight(1f),
            )

            // minimalize, maximize, close buttons
            WindowControls(
                isFloating = isFloating,
                onMinimize = onMinimize,
                onToggleMaximize = onToggleMaximize,
                onClose = onClose,
            )
        }

        HorizontalDivider(
//            color = colors.divider,
            color =  MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp,
        )
    }
}