package cz.parizmat.gitcraft.feature.sidebar.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SideBarItem(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary.copy(
                alpha = 0.14f,
            )

            hovered -> MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.08f,
            )

            else -> Color.Transparent
        },
    )
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ).padding(
                horizontal = 10.dp,
                vertical = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = /TODO(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
        )

        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}