package br.acerola.manga.ui.feature.main.home.component

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.R
import br.acerola.manga.shared.dto.archive.MangaFolderDto
import br.acerola.manga.shared.util.uriToPainter
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.SmartCard

@Composable
fun MangaListItem(
    context: Context,
    folder: MangaFolderDto, onClick: () -> Unit
) {
    val coverPainter = remember(key1 = folder.coverUri) {
        folder.coverUri?.let { uri ->
            uriToPainter(context, uri)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = 120.dp)
            .padding(all = 4.dp)
    ) {
        SmartCard(
            onClick = onClick,
            type = CardType.IMAGE,
            image = coverPainter ?: painterResource(id = R.drawable.ic_launcher_background),
            modifier = Modifier
                .width(width = 80.dp)
                .fillMaxHeight(),
        )

        Spacer(modifier = Modifier.width(width = 8.dp))

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(weight = 1f), verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1
            )
            Text(
                text = stringResource(id = R.string.manga_list_item_chapter_count, folder.chapters.size),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}