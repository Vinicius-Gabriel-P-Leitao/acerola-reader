package br.acerola.comic.module.comic.component

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.CircularProgressIndicator
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
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.GroupedHeroButton
import br.acerola.comic.common.ux.component.HeroButton
import br.acerola.comic.common.ux.component.HeroNestedButton
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
    isSyncingMangadexInfo: Boolean = false,
    isSyncingMangadexChapters: Boolean = false,
    isSyncingAnilistInfo: Boolean = false,
    isSyncingComicInfo: Boolean = false,
    isSyncingComicInfoChapters: Boolean = false,
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
                isSyncingInfo = isSyncingMangadexInfo,
                isSyncingChapters = isSyncingMangadexChapters,
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnilistSection(
                isActive = syncSource == MetadataSource.ANILIST,
                onSyncInfo = onSyncAnilistInfo,
                isSyncingInfo = isSyncingAnilistInfo,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        ComicInfoSection(
            isActive = syncSource == MetadataSource.COMIC_INFO,
            hasChapters = hasComicInfoSource,
            onSyncInfo = onSyncComicInfo,
            onSyncChapters = onSyncComicInfoChapters,
            isSyncingInfo = isSyncingComicInfo,
            isSyncingChapters = isSyncingComicInfoChapters,
        )
    }
}

@Composable
private fun MangadexSection(
    isActive: Boolean,
    hasChapters: Boolean,
    onSyncInfo: () -> Unit,
    onSyncChapters: () -> Unit,
    isSyncingInfo: Boolean = false,
    isSyncingChapters: Boolean = false,
) {
    Acerola.Component.GroupedHeroButton(
        title = stringResource(id = R.string.label_mangadex_group),
        description =
            pluralStringResource(
                id = R.plurals.description_sync_mangadex_remote_info_supporting,
                count = 1,
            ),
        iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
        onClick = if (isSyncingInfo || isSyncingChapters) null else onSyncInfo,
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
                        onClick = { if (!isSyncingInfo && !isSyncingChapters) onSyncChapters() },
                        icon = {
                            AnimatedContent(
                                targetState = isSyncingChapters,
                                label = "mangadexChaptersLoadingAnimation",
                            ) { loading ->
                                if (loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.5.dp,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        },
                    )
                }
            } else {
                null
            },
        icon = {
            AnimatedContent(
                targetState = isSyncingInfo,
                label = "mangadexInfoLoadingAnimation",
            ) { loading ->
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.mangadex_v2),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        },
    )
}

@Composable
private fun AnilistSection(
    isActive: Boolean,
    onSyncInfo: () -> Unit,
    isSyncingInfo: Boolean = false,
) {
    SyncItem(
        title = stringResource(id = R.string.title_sync_anilist_remote_info),
        subtitle = stringResource(id = R.string.description_sync_anilist_remote_info),
        iconPainter = painterResource(id = R.drawable.anilist),
        iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
        isActive = isActive,
        isSyncing = isSyncingInfo,
        onClick = onSyncInfo,
    )
}

@Composable
private fun ComicInfoSection(
    isActive: Boolean,
    hasChapters: Boolean,
    onSyncInfo: () -> Unit,
    onSyncChapters: () -> Unit,
    isSyncingInfo: Boolean = false,
    isSyncingChapters: Boolean = false,
) {
    Acerola.Component.GroupedHeroButton(
        title = stringResource(id = R.string.title_sync_comic_info),
        description = stringResource(id = R.string.description_sync_comic_info),
        onClick = if (isSyncingInfo || isSyncingChapters) null else onSyncInfo,
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
                        onClick = { if (!isSyncingInfo && !isSyncingChapters) onSyncChapters() },
                        icon = {
                            AnimatedContent(
                                targetState = isSyncingChapters,
                                label = "comicInfoChaptersLoadingAnimation",
                            ) { loading ->
                                if (loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.5.dp,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoStories,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        },
                    )
                }
            } else {
                null
            },
        icon = {
            AnimatedContent(
                targetState = isSyncingInfo,
                label = "comicInfoInfoLoadingAnimation",
            ) { loading ->
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
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
    isSyncing: Boolean = false,
    onClick: () -> Unit,
) {
    Acerola.Component.HeroButton(
        title = title,
        description = subtitle,
        iconBackground = iconBackground,
        onClick = if (isSyncing) null else onClick,
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
            AnimatedContent(
                targetState = isSyncing,
                label = "syncItemLoadingAnimation",
            ) { loading ->
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp,
                    )
                } else {
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
            }
        },
    )
}
