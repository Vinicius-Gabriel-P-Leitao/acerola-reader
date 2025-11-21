package br.acerola.manga.ui.feature.chapter.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.SmartCard

@Composable
fun ChapterItem(
    chapter: ChapterFileDto, textColor: Color, onClick: () -> Unit
) {
    SmartCard(
        onClick = onClick,
        type = CardType.CONTENT,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(
                    // TODO: Criar string
                    text = "Capitulo ${chapter.chapterSort}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = textColor
                )

                if (chapter.name.isNotEmpty()) {
                    Text(
                        text = chapter.name,
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Gray,
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.width(width = 8.dp))

            Icon(
                imageVector = Icons.Outlined.RemoveRedEye,
                modifier = Modifier.size(size = 20.dp),
                contentDescription = null,
                tint = Color.Gray,
            )
        }
    }
}