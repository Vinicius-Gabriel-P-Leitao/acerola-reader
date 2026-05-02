package br.acerola.comic.module.comic.template

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.Button
import br.acerola.comic.common.ux.tokens.ShapeTokens
import br.acerola.comic.common.ux.tokens.SizeTokens
import br.acerola.comic.common.ux.tokens.SpacingTokens
import br.acerola.comic.dto.ComicDto
import br.acerola.comic.dto.history.ReadingHistoryDto
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.pattern.metadata.ComicStatus
import br.acerola.comic.ui.R
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun Comic.Template.Header(
    comic: ComicDto,
    history: ReadingHistoryDto?,
    onContinueClick: (Long?, Int) -> Unit,
) {
    val scrollState = rememberScrollState()

    var isExpanded by remember { mutableStateOf(value = false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(height = SizeTokens.ComicHeaderHeight),
        ) {
            val context = LocalContext.current
            val bannerModel = comic.directory.bannerUri ?: comic.directory.coverUri

            val placeholderPainter =
                rememberAsyncImagePainter(
                    model =
                        ImageRequest
                            .Builder(context)
                            .data(data = R.raw.placeholder_comic)
                            .build(),
                )

            AsyncImage(
                contentDescription = null,
                contentScale = ContentScale.Crop,
                model =
                    ImageRequest
                        .Builder(context = context)
                        .data(data = bannerModel)
                        .memoryCacheKey("${bannerModel}_${comic.directory.lastModified}")
                        .diskCacheKey("${bannerModel}_${comic.directory.lastModified}")
                        .crossfade(enable = true)
                        .build(),
                placeholder = placeholderPainter,
                error = placeholderPainter,
                fallback = placeholderPainter,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .blur(radius = SpacingTokens.ExtraLarge)
                        .height(height = SizeTokens.ComicHeaderBannerHeight)
                        .align(Alignment.TopCenter),
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(height = SizeTokens.ComicHeaderBannerHeight)
                        .background(
                            Brush.verticalGradient(colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)),
                        ),
            )

            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = SpacingTokens.ExtraLarge),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    AsyncImage(
                        contentDescription = stringResource(id = R.string.comic_header_cover_description),
                        contentScale = ContentScale.Crop,
                        model =
                            ImageRequest
                                .Builder(context = context)
                                .data(data = comic.directory.coverUri)
                                .memoryCacheKey("${comic.directory.coverUri}_${comic.directory.lastModified}")
                                .diskCacheKey("${comic.directory.coverUri}_${comic.directory.lastModified}")
                                .crossfade(enable = true)
                                .build(),
                        placeholder = placeholderPainter,
                        error = placeholderPainter,
                        fallback = placeholderPainter,
                        modifier =
                            Modifier
                                .clip(shape = ShapeTokens.Medium)
                                .width(width = SizeTokens.ComicHeaderCoverWidth)
                                .height(height = SizeTokens.ComicHeaderCoverHeight)
                                .background(color = MaterialTheme.colorScheme.surfaceVariant),
                    )

                    Spacer(modifier = Modifier.width(width = SpacingTokens.Large))

                    Column(
                        verticalArrangement = Arrangement.Bottom,
                        modifier =
                            Modifier
                                .height(height = SizeTokens.ComicHeaderInfoHeight)
                                .weight(weight = 1f),
                    ) {
                        Text(
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            text = comic.remoteInfo?.title ?: comic.directory.name,
                            style =
                                MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                ),
                        )

                        Text(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            text = comic.remoteInfo?.authors?.name ?: stringResource(id = R.string.comic_header_unknown),
                        )

                        Spacer(modifier = Modifier.height(height = SpacingTokens.Small))

                        Row(horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Small)) {
                            val status = ComicStatus.fromRawValue(comic.remoteInfo?.status)
                            StatusBadge(status = stringResource(id = status.stringRes))
                            comic.remoteInfo?.syncSource?.let { source ->
                                SourceBadge(source = source.displayName)
                            }
                        }
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(space = SpacingTokens.Small),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(
                        horizontal = SpacingTokens.ExtraLarge,
                        vertical = SpacingTokens.Large,
                    ),
        ) {
            comic.remoteInfo?.genre?.forEach { genre ->
                GenreBadge(text = genre.name)
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(horizontal = SpacingTokens.ExtraLarge)
                    .clickable {
                        isExpanded = !isExpanded
                    },
        ) {
            Text(
                text = stringResource(id = R.string.comic_header_synopsis_title),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )

            Spacer(modifier = Modifier.height(height = SpacingTokens.Small))

            // TODO: Transforma resse texto + "ler mais" em um card simples colapsavel que vai ter uma seta no meio, algo como um retangulo com uma
            //  seta de drop
            Text(
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                text = comic.remoteInfo?.description ?: stringResource(id = R.string.comic_header_no_description),
            )

            Text(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = SpacingTokens.ExtraSmall),
                text =
                    if (isExpanded) {
                        stringResource(
                            id = R.string.comic_header_read_less,
                        )
                    } else {
                        stringResource(id = R.string.comic_header_read_more)
                    },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            )

            Spacer(modifier = Modifier.height(height = SpacingTokens.Small))

            // TODO: Tranformar em um Split buttons, ter a função de reler, continuar, iniciar + a de bookmark ai o icone é o que está no comic e
            //  não tiver nenhum fica um cinza ou adicionar o icone de bookmark  ao AsyncImage que fica melhor igual a quando é na tela de Home
            Acerola.Component.Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (history != null) {
                        onContinueClick(history.chapterArchiveId, history.lastPage)
                    } else {
                        onContinueClick(-1L, 0)
                    }
                },
                text = when {
                    history?.isCompleted == true -> stringResource(id = R.string.label_comic_action_reread)
                    history != null -> stringResource(id = R.string.label_comic_action_continue)
                    else -> stringResource(id = R.string.label_comic_action_start)
                },
            )
        }
    }
}

@Composable
private fun GenreBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(shape = ShapeTokens.Full)
                .background(color = MaterialTheme.colorScheme.secondaryContainer)
                .padding(
                    horizontal = SpacingTokens.Large,
                    vertical = 6.dp,
                ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SourceBadge(
    source: String,
    modifier: Modifier = Modifier,
) {

    Box(
        modifier =
            modifier
                .clip(shape = ShapeTokens.ExtraSmall)
                .background(
                    color = // FIXME: FAzer isso derivar do pattern, isso aqui hardcoded tá errado
                        when (source) {
                            "COMIC_INFO" -> MaterialTheme.colorScheme.secondaryContainer
                            "MANGADEX" -> MaterialTheme.colorScheme.tertiaryContainer
                            "ANILIST" -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                )
                .border(
                    width = SizeTokens.BorderThin,
                    color = MaterialTheme.colorScheme.outline,
                    shape = ShapeTokens.ExtraSmall,
                )
                .padding(horizontal = SpacingTokens.Small, vertical = 2.dp),
    ) {
        Text(
            text = source,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(shape = ShapeTokens.ExtraSmall)
                .background(color = MaterialTheme.colorScheme.primaryContainer)
                .border(
                    width = SizeTokens.BorderThin,
                    color = MaterialTheme.colorScheme.outline,
                    shape = ShapeTokens.ExtraSmall,
                )
                .padding(horizontal = SpacingTokens.Small, vertical = 2.dp),
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
