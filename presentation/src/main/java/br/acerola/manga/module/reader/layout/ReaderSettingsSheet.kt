package br.acerola.manga.module.reader.layout

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.config.preference.ReadingMode
import br.acerola.manga.presentation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    onDismissRequest: () -> Unit,
    currentMode: ReadingMode,
    onModeSelected: (ReadingMode) -> Unit
) {
    val glassColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    ModalBottomSheet(
        containerColor = Color.Transparent,
        onDismissRequest = onDismissRequest,
        contentWindowInsets = { WindowInsets.navigationBars },
        scrimColor = Color.Black.copy(alpha = 0.4f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(30.dp)
                        } else {
                            Modifier
                        }
                    )
                    .background(glassColor)
                    .border(
                        width = 0.5.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.label_reader_config_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                ReadingModeItem(
                    title = stringResource(id = R.string.label_reader_mode_horizontal),
                    icon = Icons.Default.AutoStories, // Better for "Paged"
                    isSelected = currentMode == ReadingMode.HORIZONTAL,
                    onClick = { onModeSelected(ReadingMode.HORIZONTAL) }
                )

                ReadingModeItem(
                    title = stringResource(id = R.string.label_reader_mode_vertical),
                    icon = Icons.Default.ViewHeadline, // Better for "Vertical scroll"
                    isSelected = currentMode == ReadingMode.VERTICAL,
                    onClick = { onModeSelected(ReadingMode.VERTICAL) }
                )

                ReadingModeItem(
                    title = stringResource(id = R.string.label_reader_mode_webtoon),
                    icon = Icons.Default.VerticalAlignBottom,
                    isSelected = currentMode == ReadingMode.WEBTOON,
                    onClick = { onModeSelected(ReadingMode.WEBTOON) }
                )
            }
        }
    }
}

@Composable
private fun ReadingModeItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            color = contentColor,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
