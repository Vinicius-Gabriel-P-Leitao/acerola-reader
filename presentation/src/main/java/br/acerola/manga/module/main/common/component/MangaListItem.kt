package br.acerola.manga.module.main.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.Card
import br.acerola.manga.common.ux.component.CardType
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.module.main.Main
import br.acerola.manga.presentation.R
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import coil.size.SizeResolver

@Composable
fun Main.Component.MangaListItem(
    manga: MangaDto,
    subtitle: String? = null,
    isCompleted: Boolean = false,
    onPlayClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val coverUri = manga.directory.coverUri ?: manga.directory.bannerUri
    val title = manga.remoteInfo?.title ?: manga.directory.name

    val imageSize = with(receiver = density) { Size(width = 80.dp.toPx().toInt(), height = 120.dp.toPx().toInt()) }

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
        model = ImageRequest.Builder(context)
            .data(data = coverUri)
            .size(resolver = SizeResolver(imageSize))
            .build(),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = 120.dp)
            .padding(all = 4.dp)
    ) {
        Acerola.Component.Card(
            onClick = onClick,
            type = CardType.IMAGE,
            image = coverPainter,
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
                text = title, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (isCompleted || subtitle != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCompleted) {
                        Text(
                            text = stringResource(id = R.string.label_manga_status_read),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.extraSmall
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (onPlayClick != null) {
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(id = R.string.description_icon_continue_reading),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
