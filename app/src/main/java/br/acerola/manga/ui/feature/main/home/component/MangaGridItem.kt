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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.acerola.manga.R
import br.acerola.manga.shared.dto.manga.MangaDto
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.SmartCard
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import coil.size.SizeResolver

@Composable
fun MangaGridItem(
    manga: MangaDto,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val coverUri = manga.folder.bannerUri ?: manga.folder.coverUri
    val title = manga.metadata?.title ?: manga.folder.name

    val imageRequest = remember(key1 = coverUri) {
        val imageSize: Size = with(receiver = density) {
            Size(
                width = 120.dp.toPx().toInt(),
                height = 180.dp.toPx().toInt()
            )
        }
        ImageRequest.Builder(context)
            .data(data = coverUri)
            .size(resolver = SizeResolver(imageSize))
            .build()
    }

    val coverPainter = rememberAsyncImagePainter(
        model = imageRequest,
        placeholder = painterResource(id = R.drawable.ic_launcher_background),
        error = painterResource(id = R.drawable.ic_launcher_background)
    )

    Column(
        modifier = Modifier.padding(all = 4.dp)
    ) {
        SmartCard(
            onClick = onClick,
            type = CardType.IMAGE,
            image = coverPainter,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = 2f / 3f),
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}