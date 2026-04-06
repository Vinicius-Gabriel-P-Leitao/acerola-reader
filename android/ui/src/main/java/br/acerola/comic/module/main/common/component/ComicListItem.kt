package br.acerola.comic.module.main.common.component
import br.acerola.comic.ui.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.BookmarkRibbon
import br.acerola.comic.common.ux.component.ImageCard
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.module.main.Main
import br.acerola.comic.pattern.MetadataSourcePattern
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import coil.size.SizeResolver

@Composable
fun Main.Common.Component.ComicListItem(
    manga: ComicDto,
    subtitle: String? = null,
    chapterCount: Int = 0,
    isCompleted: Boolean = false,
    onPlayClick: (() -> Unit)? = null,
    onShowActions: (() -> Unit)? = null,
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
            .memoryCacheKey("${coverUri}_${manga.directory.lastModified}")
            .diskCacheKey("${coverUri}_${manga.directory.lastModified}")
            .size(resolver = SizeResolver(imageSize))
            .build(),
    )

    val categoryColor = manga.category?.color
    val score = manga.remoteInfo?.sources?.anilist?.averageScore?.let { it / 10f }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = 128.dp)
            .padding(all = 4.dp)
    ) {
        // Cover Box (80dp width)
        Box(
            modifier = Modifier
                .width(width = 80.dp)
                .fillMaxHeight()
        ) {
            Acerola.Component.ImageCard(
                onClick = onClick,
                image = coverPainter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp)
            )

            // Scrim only at the bottom for the source logo visibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .align(Alignment.BottomCenter)
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                        )
                    )
            )

            // Bottom Right: Source Logo Overlay on Image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 4.dp, end = 4.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                val sourceIcon = when (manga.remoteInfo?.syncSource) {
                    MetadataSourcePattern.MANGADEX -> R.drawable.mangadex_v2
                    MetadataSourcePattern.ANILIST -> R.drawable.anilist
                    else -> null
                }
                if (sourceIcon != null) {
                    Icon(
                        painter = painterResource(id = sourceIcon),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            if (categoryColor != null) {
                BookmarkRibbon(
                    color = Color(categoryColor),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp)
                        .width(12.dp)
                        .height(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(width = 12.dp))

        // Info Column
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(weight = 1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Completed status
                if (isCompleted) {
                    Text(
                        text = stringResource(id = R.string.label_manga_status_read),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Rating
                if (score != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = score.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Chapter Count
                if (chapterCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = chapterCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(id = R.string.description_icon_continue_reading),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        if (onShowActions != null) {
            IconButton(
                onClick = onShowActions,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
