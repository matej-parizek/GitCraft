package cz.parizmat.gitcraft.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cz.parizmat.gitcraft.core.ui.components.Watermark
import cz.parizmat.gitcraft.feature.sidebar.domain.enums.Destinations
import cz.parizmat.gitcraft.feature.sidebar.ui.SidebarModelView
import org.koin.compose.koinInject


@Composable
fun MainContent(
    view: SidebarModelView = koinInject(),
    modifier: Modifier,
) {
    val currentDestination by view.state.collectAsState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (currentDestination.currentDestination) {
            Destinations.CHANGES -> {
                ChangesContent()
            }

            Destinations.HISTORY -> {
                HistoryContent()
            }

            Destinations.BRANCHES -> {
                BranchesContent()
            }

            else -> {
                Watermark()
            }
        }
    }
}

@Composable
private fun ChangesContent() {
    Watermark()
}

@Composable
private fun HistoryContent() {
    Watermark()
}

@Composable
private fun BranchesContent() {
    Watermark()
}
