package cz.parizmat.gitcraft.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cz.parizmat.gitcraft.core.ui.components.Watermark
import cz.parizmat.gitcraft.feature.sidebar.ui.MainSidebar

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Watermark(modifier = Modifier.fillMaxSize())

        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            MainSidebar(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
            MainContent(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(4f)
            )
        }
    }
}
