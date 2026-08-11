package br.acerola.comic.module.comic.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.Dialog
import br.acerola.comic.common.ux.component.DialogButton
import br.acerola.comic.common.ux.tokens.ShapeTokens
import br.acerola.comic.common.ux.tokens.SizeTokens
import br.acerola.comic.common.ux.tokens.SpacingTokens
import br.acerola.comic.dto.archive.ChapterFileDto
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.ui.R
import androidx.compose.ui.tooling.preview.Preview
import android.content.res.Configuration
import br.acerola.comic.common.ux.theme.AcerolaTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Comic.Component.ChapterItem(
    chapterFileDto: ChapterFileDto,
    modifier: Modifier = Modifier,
    onToggleRead: () -> Unit = {},
    isRead: Boolean = false,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit,
) {
    var showDetails by remember { mutableStateOf(value = false) }

    val mainTitle = stringResource(id = R.string.title_chapter_item_chapter_number, chapterFileDto.chapterSort)
    val subtitle = chapterFileDto.name

    val iconBackground =
        if (isRead) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val iconTint =
        if (isRead) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    val surfaceColor =
        if (isRead) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        } else {
            Color.Transparent
        }

    Surface(
        color = surfaceColor,
        modifier =
            modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SpacingTokens.ExtraLarge, vertical = SpacingTokens.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode) {
                if (isSelected) {
                    Box(
                        modifier =
                            Modifier
                                .size(SizeTokens.ClickTargetSmall)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(SizeTokens.IconSmall),
                        )
                    }
                } else {
                    Box(
                        modifier =
                            Modifier
                                .size(SizeTokens.ClickTargetSmall)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape,
                                ).border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                ),
                    )
                }
            } else {
                Surface(
                    shape = ShapeTokens.MediumLarge,
                    color = iconBackground,
                    modifier = Modifier.size(SizeTokens.ClickTargetSmall),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isRead) Icons.Default.CheckCircle else Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(SizeTokens.IconSmall),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(SpacingTokens.Large))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mainTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = { if (isSelectionMode) onClick() else showDetails = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.description_icon_chapter_more_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showDetails) {
        Acerola.Component.Dialog(
            show = true,
            title = mainTitle,
            onDismiss = { showDetails = false },
            confirmButtonContent = {
                Acerola.Component.DialogButton(
                    text = stringResource(id = R.string.label_dialog_close),
                    onClick = { showDetails = false },
                )
            },
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DetailRow(
                        label = stringResource(id = R.string.label_chapter_detail_file),
                        value = chapterFileDto.name,
                    )

                    Spacer(modifier = Modifier.height(SpacingTokens.Large))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(SpacingTokens.Small))

                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onToggleRead() },
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = if (isRead) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            ),
                    ) {
                        Text(
                            text =
                                if (isRead) {
                                    stringResource(id = R.string.action_mark_as_unread)
                                } else {
                                    stringResource(id = R.string.action_mark_as_read)
                                },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    if (value.isBlank()) return
    Column(modifier = Modifier.padding(bottom = SpacingTokens.Medium)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChapterItemPreview() {
    AcerolaTheme {
        Comic.Component.ChapterItem(
            chapterFileDto = ChapterFileDto(id = 1L, name = "Capítulo 1", path = "/path/1", chapterSort = "0001"),
            onClick = {},
        )
    }
}
