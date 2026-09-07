package cz.parizmat.gitcraft.feature.main.toolbar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.parizmat.gitcraft.core.ui.components.Logo
import io.github.lyxnx.compose.ui.tablericons.TablerIcons
import io.github.lyxnx.compose.ui.tablericons.filled.ChevronDown

@Composable
internal fun ProjectChip(
    label: String,
    colors: IdeToolbarColors,
    onClick: () -> Unit,
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    val hovered by interactionSource.collectIsHoveredAsState()

    val background by animateColorAsState(
        targetValue = if (hovered) {
            colors.hoverOverlay
        } else {
            Color.Transparent
        },
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = 6.dp,
                vertical = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {


        Spacer(
            modifier = Modifier.width(6.dp),
        )

        Text(
            text = label,
            color = colors.textPrimary,
            fontSize = 13.sp,
        )

        Spacer(
            modifier = Modifier.width(2.dp),
        )


    }
}

@Composable
internal fun DropdownChip(
    icon: ImageVector,
    label: String,
    colors: IdeToolbarColors,
    onClick: () -> Unit,
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    val hovered by interactionSource.collectIsHoveredAsState()

    val background by animateColorAsState(
        targetValue = if (hovered) {
            colors.hoverOverlay
        } else {
            Color.Transparent
        },
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = 6.dp,
                vertical = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.iconTint,
            modifier = Modifier.size(14.dp),
        )

        Spacer(
            modifier = Modifier.width(6.dp),
        )

        Text(
            text = label,
            color = colors.textPrimary,
            fontSize = 13.sp,
        )

        Spacer(
            modifier = Modifier.width(2.dp),
        )

        Icon(
            imageVector = TablerIcons.Filled.ChevronDown,
            contentDescription = null,
            tint = colors.iconTint,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
internal fun IconGhostButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
        .size(28.dp)
        .clip(RoundedCornerShape(6.dp)),
    iconModifier: Modifier = Modifier.size(14.dp),
    showBadge: Boolean = false,
    hooverColor: Color? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    val  scheme = MaterialTheme.colorScheme
    val hovered by interactionSource.collectIsHoveredAsState()

    val background by animateColorAsState(
        targetValue = if (hovered) {
            hooverColor ?: scheme.onSurface.copy(
                alpha = 0.08f,
            )
        } else {
            Color.Transparent
        },
    )

    Box(
        modifier = modifier
            .background(background)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = iconModifier,
        )

        if (showBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        x = (-2).dp,
                        y = 2.dp,
                    )
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF3574F0)),
            )
        }
    }
}

@Composable
internal fun ToolbarDivider(
    colors: IdeToolbarColors,
) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(18.dp)
            .background(colors.divider),
    )
}

@Composable
internal fun StartTrialButton(
    colors: IdeToolbarColors,
    onClick: () -> Unit,
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    val hovered by interactionSource.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        targetValue = if (hovered) {
            colors.accentGreen
        } else {
            colors.accentGreen.copy(
                alpha = 0.6f,
            )
        },
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(50),
            )
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = 14.dp,
                vertical = 5.dp,
            ),
    ) {
        Text(
            text = "Start Free Trial",
            color = colors.accentGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}