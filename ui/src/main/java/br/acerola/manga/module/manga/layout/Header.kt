package br.acerola.manga.module.manga.layout

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.Button
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.history.ReadingHistoryDto
import br.acerola.manga.module.manga.Manga
import br.acerola.manga.ui.R
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun Manga.Layout.Header(
    manga: MangaDto,
    history: ReadingHistoryDto?,
    onContinueClick: (Long, Int) -> Unit
) {
    val scrollState = rememberScrollState()

    var isExpanded by remember { mutableStateOf(value = false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = 420.dp)
        ) {
            val context = LocalContext.current
            val bannerModel = manga.directory.bannerUri ?: manga.directory.coverUri

            val placeholderPainter = coil.compose.rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(data = R.raw.placeholder_manga)
                    .build()
            )

            AsyncImage(
                contentDescription = null,
                contentScale = ContentScale.Crop,
                model = ImageRequest.Builder(context = context)
                    .data(data = bannerModel)
                    .memoryCacheKey("${bannerModel}_${manga.directory.lastModified}")
                    .diskCacheKey("${bannerModel}_${manga.directory.lastModified}")
                    .crossfade(enable = true)
                    .build(),
                placeholder = placeholderPainter,
                error = placeholderPainter,
                fallback = placeholderPainter,
                modifier = Modifier
                    .fillMaxWidth()
                    .blur(radius = 20.dp)
                    .height(height = 350.dp)
                    .align(Alignment.TopCenter)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 350.dp)
                    .background(
                        Brush.verticalGradient(colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background))
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom
                ) {
                    AsyncImage(
                        contentDescription = stringResource(id = R.string.manga_header_cover_description),
                        contentScale = ContentScale.Crop,
                        model = ImageRequest.Builder(context = context)
                            .data(data = manga.directory.coverUri)
                            .memoryCacheKey("${manga.directory.coverUri}_${manga.directory.lastModified}")
                            .diskCacheKey("${manga.directory.coverUri}_${manga.directory.lastModified}")
                            .crossfade(enable = true)
                            .build(),
                        placeholder = placeholderPainter,
                        error = placeholderPainter,
                        fallback = placeholderPainter,
                        modifier = Modifier
                            .clip(shape = RoundedCornerShape(size = 12.dp))
                            .width(width = 130.dp)
                            .height(height = 190.dp)
                            .background(color = MaterialTheme.colorScheme.surfaceVariant)
                    )

                    Spacer(modifier = Modifier.width(width = 16.dp))

                    Column(
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .height(height = 170.dp)
                            .weight(weight = 1f),
                    ) {
                        Text(
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            text = manga.remoteInfo?.title ?: manga.directory.name,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground
                            ),
                        )

                        Text(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            text = manga.remoteInfo?.authors?.name
                                ?: stringResource(id = R.string.manga_header_unknown),
                        )

                        Spacer(modifier = Modifier.height(height = 8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusBadge(
                                status = manga.remoteInfo?.status ?: stringResource(id = R.string.manga_header_unknown)
                            )
                            manga.remoteInfo?.syncSource?.let { source ->
                                SourceBadge(source = source.displayName)
                            }
                        }
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(
                    horizontal = 20.dp, vertical = 16.dp
                ),
        ) {
            manga.remoteInfo?.genre?.forEach { genre ->
                GenreBadge(text = genre.name)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = 20.dp)
                .clickable {
                    isExpanded = !isExpanded
                }) {
            Text(
                text = stringResource(id = R.string.manga_header_synopsis_title),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )

            Spacer(modifier = Modifier.height(height = 8.dp))

            Text(
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                text = manga.remoteInfo?.description ?: stringResource(id = R.string.manga_header_no_description),
            )

            Text(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
                text = if (isExpanded) stringResource(id = R.string.manga_header_read_less) else stringResource(id = R.string.manga_header_read_more),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            )

            Spacer(modifier = Modifier.height(height = 8.dp))

            val buttonText = when {
                history?.isCompleted == true -> stringResource(id = R.string.label_manga_action_reread)
                history != null -> stringResource(id = R.string.label_manga_action_continue)
                else -> stringResource(id = R.string.label_manga_action_start)
            }
            
            Acerola.Component.Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (history != null) {
                        onContinueClick(history.chapterArchiveId, history.lastPage)
                    } else {
                        onContinueClick(-1L, 0)
                    }
                },
                text = buttonText
            )
        }
    }
}

@Composable
private fun GenreBadge(
    text: String, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(shape = RoundedCornerShape(percent = 50))
            .background(color = MaterialTheme.colorScheme.secondaryContainer)
            .padding(
                horizontal = 16.dp, vertical = 6.dp
            )
    ) {
        Text(
            text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SourceBadge(
    source: String, modifier: Modifier = Modifier
) {
    val color = when (source) {
        "COMIC_INFO" -> MaterialTheme.colorScheme.secondaryContainer
        "MANGADEX" -> MaterialTheme.colorScheme.tertiaryContainer
        "ANILIST" -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 4.dp))
            .background(color = color)
            .border(
                width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(size = 4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = source,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusBadge(
    status: String, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 4.dp))
            .background(color = MaterialTheme.colorScheme.primaryContainer)
            .border(
                width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(size = 4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
