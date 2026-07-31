package br.acerola.comic.module.comic.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.state.SyncActionVisualState
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.GroupedHeroButton
import br.acerola.comic.common.ux.component.HeroButton
import br.acerola.comic.common.ux.component.HeroNestedButton
import br.acerola.comic.common.ux.component.SyncActionIcon
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.pattern.metadata.MetadataSource
import br.acerola.comic.ui.R

@Composable
fun Comic.Component.SyncMetadata(
    remoteInfo: ComicMetadataDto?,
    externalSyncEnabled: Boolean,
    onSyncMangadexInfo: () -> Unit,
    onSyncMangadexChapters: () -> Unit,
    onSyncComicInfo: () -> Unit,
    onSyncComicInfoChapters: () -> Unit,
    onSyncAnilistInfo: () -> Unit,
    mangadexInfoState: SyncActionVisualState = SyncActionVisualState.IDLE,
    mangadexChaptersState: SyncActionVisualState = SyncActionVisualState.IDLE,
    anilistInfoState: SyncActionVisualState = SyncActionVisualState.IDLE,
    comicInfoState: SyncActionVisualState = SyncActionVisualState.IDLE,
    comicInfoChaptersState: SyncActionVisualState = SyncActionVisualState.IDLE,
    modifier: Modifier = Modifier,
) {
    val syncSource = remoteInfo?.syncSource
    val hasMangadexSource = remoteInfo?.sources?.mangadex?.mangadexId != null
    val hasComicInfoSource = remoteInfo?.sources?.comicInfo?.localHash != null

    Column(modifier = modifier) {
        if (externalSyncEnabled) {
            MangadexSection(
                isActive = syncSource == MetadataSource.MANGADEX,
                hasChapters = hasMangadexSource && remoteInfo.id != null,
                onSyncInfo = onSyncMangadexInfo,
                onSyncChapters = onSyncMangadexChapters,
                infoState = mangadexInfoState,
                chaptersState = mangadexChaptersState,
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnilistSection(
                isActive = syncSource == MetadataSource.ANILIST,
                onSyncInfo = onSyncAnilistInfo,
                infoState = anilistInfoState,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        ComicInfoSection(
            isActive = syncSource == MetadataSource.COMIC_INFO,
            hasChapters = hasComicInfoSource,
            onSyncInfo = onSyncComicInfo,
            onSyncChapters = onSyncComicInfoChapters,
            infoState = comicInfoState,
            chaptersState = comicInfoChaptersState,
        )
    }
}

@Composable
private fun MangadexSection(
    isActive: Boolean,
    hasChapters: Boolean,
    onSyncInfo: () -> Unit,
    onSyncChapters: () -> Unit,
    infoState: SyncActionVisualState = SyncActionVisualState.IDLE,
    chaptersState: SyncActionVisualState = SyncActionVisualState.IDLE,
) {
    val anyLoading = infoState == SyncActionVisualState.LOADING || chaptersState == SyncActionVisualState.LOADING

    Acerola.Component.GroupedHeroButton(
        title = stringResource(id = R.string.label_mangadex_group),
        description =
            pluralStringResource(
                id = R.plurals.description_sync_mangadex_remote_info_supporting,
                count = 1,
            ),
        iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
        onClick = if (anyLoading) null else onSyncInfo,
        action =
            if (isActive) {
                {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                null
            },
        nestedItem =
            if (hasChapters && isActive) {
                {
                    Acerola.Component.HeroNestedButton(
                        title = stringResource(id = R.string.title_sync_chapters),
                        description = stringResource(id = R.string.description_sync_chapters_remote),
                        iconBackground = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { if (!anyLoading) onSyncChapters() },
                        icon = {
                            Acerola.Component.SyncActionIcon(
                                state = chaptersState,
                                containerSize = 40.dp,
                                iconSize = 20.dp,
                                defaultBackground = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                    )
                }
            } else {
                null
            },
        icon = {
            Acerola.Component.SyncActionIcon(
                state = infoState,
                defaultBackground = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mangadex_v2),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
    )
}

@Composable
private fun AnilistSection(
    isActive: Boolean,
    onSyncInfo: () -> Unit,
    infoState: SyncActionVisualState = SyncActionVisualState.IDLE,
) {
    SyncItem(
        title = stringResource(id = R.string.title_sync_anilist_remote_info),
        subtitle = stringResource(id = R.string.description_sync_anilist_remote_info),
        iconPainter = painterResource(id = R.drawable.anilist),
        iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
        isActive = isActive,
        state = infoState,
        onClick = onSyncInfo,
    )
}

@Composable
private fun ComicInfoSection(
    isActive: Boolean,
    hasChapters: Boolean,
    onSyncInfo: () -> Unit,
    onSyncChapters: () -> Unit,
    infoState: SyncActionVisualState = SyncActionVisualState.IDLE,
    chaptersState: SyncActionVisualState = SyncActionVisualState.IDLE,
) {
    val anyLoading = infoState == SyncActionVisualState.LOADING || chaptersState == SyncActionVisualState.LOADING

    Acerola.Component.GroupedHeroButton(
        title = stringResource(id = R.string.title_sync_comic_info),
        description = stringResource(id = R.string.description_sync_comic_info),
        onClick = if (anyLoading) null else onSyncInfo,
        action =
            if (isActive) {
                {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                null
            },
        nestedItem =
            if (hasChapters && isActive) {
                {
                    Acerola.Component.HeroNestedButton(
                        title = stringResource(id = R.string.title_sync_chapters),
                        description = stringResource(id = R.string.description_sync_chapters_internal),
                        iconBackground = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { if (!anyLoading) onSyncChapters() },
                        icon = {
                            Acerola.Component.SyncActionIcon(
                                state = chaptersState,
                                containerSize = 40.dp,
                                iconSize = 20.dp,
                                defaultBackground = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoStories,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                    )
                }
            } else {
                null
            },
        icon = {
            Acerola.Component.SyncActionIcon(
                state = infoState,
                defaultBackground = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
    )
}

@Composable
private fun SyncItem(
    title: String,
    subtitle: String,
    iconVector: ImageVector? = null,
    iconPainter: Painter? = null,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    isActive: Boolean = false,
    state: SyncActionVisualState = SyncActionVisualState.IDLE,
    onClick: () -> Unit,
) {
    Acerola.Component.HeroButton(
        title = title,
        description = subtitle,
        iconBackground = iconBackground,
        onClick = if (state == SyncActionVisualState.LOADING) null else onClick,
        action =
            if (isActive) {
                {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                null
            },
        icon = {
            Acerola.Component.SyncActionIcon(
                state = state,
                defaultBackground = iconBackground,
            ) {
                when {
                    iconVector != null ->
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint =
                                if (iconBackground == MaterialTheme.colorScheme.tertiaryContainer) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                            modifier = Modifier.size(24.dp),
                        )
                    iconPainter != null ->
                        Image(
                            painter = iconPainter,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                }
            }
        },
    )
}
