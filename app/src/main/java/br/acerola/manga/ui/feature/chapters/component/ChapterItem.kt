package br.acerola.manga.ui.feature.chapters.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.ui.common.component.ButtonType
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.SmartButton
import br.acerola.manga.ui.common.component.SmartCard

@Composable
fun ChapterItem(
    chapter: ChapterFileDto, onClick: (ChapterFileDto) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
    ) {
        SmartCard(
            type = CardType.CONTENT, modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            ) {
                Text(
                    text = chapter.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(weight = 1f)
                )

                SmartButton(
                    type = ButtonType.ICON, onClick = { onClick(chapter) }, modifier = Modifier.size(size = 36.dp)
                ) { Icon(imageVector = Icons.Default.CheckCircleOutline, contentDescription = "Ler capítulo") }
            }
        }
    }
}