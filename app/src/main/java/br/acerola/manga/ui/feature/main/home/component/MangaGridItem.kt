package br.acerola.manga.ui.feature.main.home.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.acerola.manga.R
import br.acerola.manga.shared.dto.archive.MangaFolderDto
import br.acerola.manga.shared.util.uriToPainter
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.SmartCard

@Composable
fun MangaGridItem(
    folder: MangaFolderDto,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val coverPainter = remember(key1 = folder.coverUri) {
        folder.coverUri?.let { uri ->
            uriToPainter(context, uri)
        }
    }

    Column(
        modifier = Modifier.padding(all = 4.dp)
    ) {
        SmartCard(
            onClick = onClick,
            type = CardType.IMAGE,
            image = coverPainter ?: painterResource(id = R.drawable.ic_launcher_background),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = 2f / 3f),
        )

        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}