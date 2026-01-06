package br.acerola.manga.module.manga.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.dto.archive.ChapterFileDto
import br.acerola.manga.dto.metadata.chapter.ChapterFeedDto
import br.acerola.manga.feature.R

@Composable
fun ChapterItem(
    chapterRemoteInfoDto: ChapterFeedDto?,
    chapterFileDto: ChapterFileDto,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val stableOnClick = remember(key1 = chapterFileDto.id) { onClick }

    SmartCard(
        onClick = stableOnClick,
        type = CardType.CONTENT,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    text = chapterRemoteInfoDto?.title?.takeIf { it.isNotBlank() }
                        ?: stringResource(id = R.string.title_chapter_item_chapter_number, chapterFileDto.chapterSort)
                )

                if (chapterFileDto.name.isNotEmpty()) {
                    Text(
                        text = chapterFileDto.name,
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.width(width = 8.dp))

            Icon(
                imageVector = Icons.Outlined.RemoveRedEye,
                modifier = Modifier.size(size = 20.dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}