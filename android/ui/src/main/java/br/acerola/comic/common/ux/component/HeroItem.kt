package br.acerola.comic.common.ux.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola

private val HeroShape = RoundedCornerShape(24.dp)
private val IconShape = RoundedCornerShape(16.dp)

@Composable
fun Acerola.Component.HeroItem(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    description: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
    bottomContent: @Composable (() -> Unit)? = null,
) {
    Acerola.Component.HeroItem(
        title = title,
        modifier = modifier,
        description = description,
        iconBackground = iconBackground,
        onClick = onClick,
        action = action,
        bottomContent = bottomContent,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        },
    )
}

@Composable
fun Acerola.Component.HeroItem(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
    bottomContent: @Composable (() -> Unit)? = null,
    icon: @Composable () -> Unit,
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = HeroShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier.fillMaxWidth(),
        ) {
            HeroItemContent(title, description, iconBackground, action, bottomContent, icon)
        }
    } else {
        Surface(
            shape = HeroShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier.fillMaxWidth(),
        ) {
            HeroItemContent(title, description, iconBackground, action, bottomContent, icon)
        }
    }
}

@Composable
fun Acerola.Component.HeroNestedItem(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = iconBackground,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun Acerola.Component.HeroNestedItem(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    description: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: () -> Unit,
) {
    Acerola.Component.HeroNestedItem(
        title = title,
        description = description,
        modifier = modifier,
        iconBackground = iconBackground,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        },
    )
}

@Composable
fun Acerola.Component.GroupedHeroItem(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
    nestedItem: @Composable (() -> Unit)? = null,
    icon: @Composable () -> Unit,
) {
    Acerola.Component.HeroItem(
        title = title,
        modifier = modifier,
        description = description,
        iconBackground = iconBackground,
        onClick = onClick,
        action = action,
        bottomContent = nestedItem,
        icon = icon,
    )
}

@Composable
fun Acerola.Component.GroupedHeroItem(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    description: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
    nestedItem: @Composable (() -> Unit)? = null,
) {
    Acerola.Component.HeroItem(
        title = title,
        icon = icon,
        modifier = modifier,
        description = description,
        iconTint = iconTint,
        iconBackground = iconBackground,
        onClick = onClick,
        action = action,
        bottomContent = nestedItem,
    )
}

@Composable
private fun HeroItemContent(
    title: String,
    description: String?,
    iconBackground: Color,
    action: @Composable (() -> Unit)?,
    bottomContent: @Composable (() -> Unit)?,
    icon: @Composable () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = IconShape,
                color = iconBackground,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (action != null) {
                Spacer(modifier = Modifier.width(12.dp))
                action()
            }
        }

        if (bottomContent != null) {
            HorizontalDivider(
                modifier =
                    Modifier
                        .padding(horizontal = 20.dp)
                        .alpha(0.4f),
            )
            bottomContent()
        }
    }
}
