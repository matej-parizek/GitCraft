package cz.parizmat.gitcraft.feature.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.parizmat.gitcraft.core.ui.components.Logo

@Composable
fun GitCraftToolBar(projectName: String, branchName: String, isFloating: Boolean, onMinimize: () -> Unit, onToggleMaximize: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(44.dp).background(MaterialTheme.colorScheme.surface).padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Logo(Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("GitCraft", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(24.dp))
            Text(projectName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(branchName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            WindowControls(isFloating = isFloating, onMinimize = onMinimize, onToggleMaximize = onToggleMaximize, onClose = onClose)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}
