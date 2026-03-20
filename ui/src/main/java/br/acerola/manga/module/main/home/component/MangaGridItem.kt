package br.acerola.manga.module.main.home.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.BookmarkRibbon
import br.acerola.manga.common.ux.component.ImageCard
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.module.main.Main
import br.acerola.manga.ui.R
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import coil.size.SizeResolver

@Composable
fun Main.Home.Component.MangaGridItem(
    manga: MangaDto,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val coverUri = manga.directory.coverUri ?: manga.directory.bannerUri
    val title = manga.remoteInfo?.title ?: manga.directory.name

    val imageSize: Size = with(receiver = density) {
        Size(
            width = 120.dp.toPx().toInt(),
            height = 180.dp.toPx().toInt()
        )
    }

    val placeholderPainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(data = R.raw.placeholder_manga)
            .size(resolver = SizeResolver(imageSize))
            .build()
    )

    val coverPainter = rememberAsyncImagePainter(
        placeholder = placeholderPainter,
        fallback = placeholderPainter,
        error = placeholderPainter,
        model = ImageRequest.Builder(context).data(data = coverUri).size(resolver = SizeResolver(imageSize)).build(),
    )

    val categoryColor = manga.category?.color

    Column(
        modifier = Modifier.padding(all = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(ratio = 2f / 3f)) {
            Acerola.Component.ImageCard(
                onClick = onClick,
                image = coverPainter,
                modifier = Modifier.fillMaxSize(),
            )

            if (categoryColor != null) {
                BookmarkRibbon(
                    color = Color(categoryColor),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 12.dp)
                        .width(16.dp)
                        .height(24.dp)
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
