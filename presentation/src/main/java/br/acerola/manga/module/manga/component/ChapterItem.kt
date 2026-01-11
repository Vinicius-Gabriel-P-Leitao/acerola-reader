package br.acerola.manga.module.manga.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.ModalDialog
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.metadata.chapter.ChapterFeedDto
import br.acerola.manga.presentation.R

@Composable
fun ChapterItem(
    chapterRemoteInfoDto: ChapterFeedDto?,
    chapterFileDto: ChapterFileDto,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var showDetails by remember { mutableStateOf(value = false) }
    val stableOnClick = remember(key1 = chapterFileDto.id) { onClick }

    val chapterNumber = chapterRemoteInfoDto?.chapter ?: chapterFileDto.chapterSort
    val mainTitle = stringResource(id = R.string.title_chapter_item_chapter_number, chapterNumber)

    val subtitle = chapterRemoteInfoDto?.title?.takeIf { it.isNotBlank() } ?: chapterFileDto.name

    SmartCard(
        type = CardType.CONTENT,
        onClick = stableOnClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(
                    text = mainTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )

                Text(
                    maxLines = 1,
                    text = subtitle,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (chapterRemoteInfoDto?.scanlation?.isNotBlank() == true) {
                    Text(
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        text = stringResource(
                            id = R.string.label_chapter_scanlation_prefix, chapterRemoteInfoDto.scanlation
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = { showDetails = true }) {
                Icon(
                    contentDescription = stringResource(id = R.string.description_icon_chapter_more_options),
                    imageVector = Icons.Default.MoreVert,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDetails) {
        ModalDialog(show = true, title = mainTitle, onDismiss = { showDetails = false }, confirmButtonContent = {
            TextButton(onClick = { showDetails = false }) {
                Text(text = stringResource(id = R.string.label_dialog_close))
            }
        }, content = {
            Column {
                DetailRow(
                    label = stringResource(id = R.string.label_chapter_detail_file), value = chapterFileDto.name
                )
                chapterRemoteInfoDto?.let { remote ->
                    if (remote.title.isNotBlank()) DetailRow(
                        label = stringResource(id = R.string.label_chapter_detail_title), value = remote.title
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
            }
        })
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface
        )
    }
}