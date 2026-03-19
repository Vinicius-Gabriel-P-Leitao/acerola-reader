package br.acerola.manga.module.manga.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.Dialog
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.metadata.chapter.ChapterFeedDto
import br.acerola.manga.module.manga.Manga
import br.acerola.manga.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Manga.Component.ChapterItem(
    chapterRemoteInfoDto: ChapterFeedDto?,
    chapterFileDto: ChapterFileDto,
    modifier: Modifier = Modifier,
    onToggleRead: () -> Unit = {},
    isRead: Boolean = false,
    onClick: () -> Unit
) {
    var showDetails by remember { mutableStateOf(value = false) }
    val stableOnClick = remember(key1 = chapterFileDto.id) { onClick }

    val chapterNumber = chapterRemoteInfoDto?.chapter ?: chapterFileDto.chapterSort
    val mainTitle = stringResource(id = R.string.title_chapter_item_chapter_number, chapterNumber)
    val subtitle = chapterRemoteInfoDto?.title?.takeIf { it.isNotBlank() } ?: chapterFileDto.name

    ElevatedCard(
        onClick = stableOnClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isRead) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Indicador lateral sutil para capítulos lidos
            if (isRead) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(4.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (isRead) 12.dp else 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = mainTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        
                        if (isRead) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (chapterRemoteInfoDto?.scanlation?.isNotBlank() == true) {
                        Text(
                            text = stringResource(
                                id = R.string.label_chapter_scanlation_prefix,
                                chapterRemoteInfoDto.scanlation
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = { showDetails = true },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(id = R.string.description_icon_chapter_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDetails) {
        Acerola.Component.Dialog(
            show = true,
            title = mainTitle,
            onDismiss = { showDetails = false },
            confirmButtonContent = {
                TextButton(onClick = { showDetails = false }) {
                    Text(
                        text = stringResource(id = R.string.label_dialog_close),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DetailRow(
                        label = stringResource(id = R.string.label_chapter_detail_file),
                        value = chapterFileDto.name
                    )
                    chapterRemoteInfoDto?.let { remote ->
                        if (remote.title.isNotBlank()) DetailRow(
                            label = stringResource(id = R.string.label_chapter_detail_title),
                            value = remote.title
                        )
                        DetailRow(
                            label = stringResource(id = R.string.label_chapter_detail_scanlation),
                            value = remote.scanlation
                        )
                        DetailRow(
                            label = stringResource(id = R.string.label_chapter_detail_pages),
                            value = "${remote.pageCount ?: "?"}"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onToggleRead()
                            showDetails = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isRead) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (isRead) stringResource(id = R.string.action_mark_as_unread) else stringResource(id = R.string.action_mark_as_read),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
