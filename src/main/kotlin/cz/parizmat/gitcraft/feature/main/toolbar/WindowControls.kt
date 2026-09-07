package cz.parizmat.gitcraft.feature.main.toolbar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun WindowControls(
    isFloating: Boolean,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit,
) {
    val (iconModifier, icon, contentDescription) = if (isFloating) {
        Triple(
            Modifier
                .size(14.dp),
            Icons.Default.CropSquare,
            "Maximize",
        )
    } else {
        Triple(
            Modifier.size(10.dp),
            Icons.Default.FilterNone,
            "Restore",
        )
    }

    val modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconGhostButton(
            icon = Icons.Default.Minimize,
            contentDescription = "Minimize",
            onClick = onMinimize,
            modifier = modifier
        )

        IconGhostButton(
            icon = icon,
            contentDescription = contentDescription,
            onClick = onToggleMaximize,
            iconModifier = iconModifier,
            modifier = modifier
        )

        IconGhostButton(
            icon = Icons.Default.Clear,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurface,
            onClick = onClose,
            modifier = modifier
                .size(48.dp, 28.dp)
                .clip(RoundedCornerShape(4.dp)),
            hooverColor = Color(0xFFDB0413)
        )
    }
}