package br.acerola.comic.module.reader.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.GlassButton
import br.acerola.comic.common.ux.modifier.glass
import br.acerola.comic.common.ux.modifier.glassContainer
import br.acerola.comic.module.reader.Reader
import br.acerola.comic.ui.R

@Composable
fun Reader.Layout.TopBar(
    title: String,
    subtitle: String,
    isVisible: Boolean,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Back Button
            Box(modifier = Modifier.size(48.dp)) {
                Acerola.Component.GlassButton(
                    onClick = onBackClick,
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.description_icon_navigation_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )
            }

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                TitleCapsule(title = title, subtitle = subtitle)
            }

            Box(modifier = Modifier.size(48.dp)) {
                Acerola.Component.GlassButton(
                    onClick = onSettingsClick,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(id = R.string.label_config_activity),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun Reader.Layout.TitleCapsule(
    title: String,
    subtitle: String,
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val glassColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier =
            Modifier
                .wrapContentWidth(Alignment.CenterHorizontally)
                .glassContainer(shape),
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .glass(shape, glassColor, borderColor),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}
