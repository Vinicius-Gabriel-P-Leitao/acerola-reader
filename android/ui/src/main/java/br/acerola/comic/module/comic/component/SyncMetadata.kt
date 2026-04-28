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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.GroupedHeroItem
import br.acerola.comic.common.ux.component.HeroItem
import br.acerola.comic.common.ux.component.HeroNestedItem
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.pattern.MetadataSourcePattern
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
    modifier: Modifier = Modifier,
) {
    val syncSource = remoteInfo?.syncSource
    val hasMangadexSource = remoteInfo?.sources?.mangadex?.mangadexId != null
    val hasComicInfoSource = remoteInfo?.sources?.comicInfo?.localHash != null

    Column(modifier = modifier) {
        if (externalSyncEnabled) {
            MangadexSection(
                isActive = syncSource == MetadataSourcePattern.MANGADEX,
                hasChapters = hasMangadexSource && remoteInfo.id != null,
                onSyncInfo = onSyncMangadexInfo,
                onSyncChapters = onSyncMangadexChapters,
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnilistSection(
                isActive = syncSource == MetadataSourcePattern.ANILIST,
                onSyncInfo = onSyncAnilistInfo,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        ComicInfoSection(
            isActive = syncSource == MetadataSourcePattern.COMIC_INFO,
            hasChapters = hasComicInfoSource,
            onSyncInfo = onSyncComicInfo,
            onSyncChapters = onSyncComicInfoChapters,
        )
    }
}

@Composable
private fun MangadexSection(
    isActive: Boolean,
    hasChapters: Boolean,
    onSyncInfo: () -> Unit,
    onSyncChapters: () -> Unit,
) {
    Acerola.Component.GroupedHeroItem(
        title = stringResource(id = R.string.label_mangadex_group),
        description =
            pluralStringResource(
                id = R.plurals.description_sync_mangadex_remote_info_supporting,
                count = 1,
            ),
        iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
        onClick = onSyncInfo,
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
                    Acerola.Component.HeroNestedItem(
                        title = stringResource(id = R.string.title_sync_chapters),
                        description = stringResource(id = R.string.description_sync_chapters_remote),
                        icon = Icons.Rounded.AutoAwesome,
                        onClick = onSyncChapters,
                    )
                }
            } else {
                null
            },
        icon = {
            Image(
                painter = painterResource(id = R.drawable.mangadex_v2),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        },
    )
}

@Composable
private fun AnilistSection(
    isActive: Boolean,
    onSyncInfo: () -> Unit,
) {
    SyncItem(
        title = stringResource(id = R.string.title_sync_anilist_remote_info),
        subtitle = stringResource(id = R.string.description_sync_anilist_remote_info),
        iconPainter = painterResource(id = R.drawable.anilist),
        iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
        isActive = isActive,
        onClick = onSyncInfo,
    )
}

@Composable
private fun ComicInfoSection(
    isActive: Boolean,
    hasChapters: Boolean,
    onSyncInfo: () -> Unit,
    onSyncChapters: () -> Unit,
) {
    Acerola.Component.GroupedHeroItem(
        title = stringResource(id = R.string.title_sync_comic_info),
        description = stringResource(id = R.string.description_sync_comic_info),
        icon = Icons.Rounded.Description,
        onClick = onSyncInfo,
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
                    Acerola.Component.HeroNestedItem(
                        title = stringResource(id = R.string.title_sync_chapters),
                        description = stringResource(id = R.string.description_sync_chapters_internal),
                        icon = Icons.Rounded.AutoStories,
                        onClick = onSyncChapters,
                    )
                }
            } else {
                null
            },
    )
}

@Composable
private fun SyncItem(
    title: String,
    subtitle: String,
    iconVector: ImageVector? = null,
    iconPainter: Painter? = null,
    iconBackground: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    isActive: Boolean = false,
    onClick: () -> Unit,
) {
    Acerola.Component.HeroItem(
        title = title,
        description = subtitle,
        iconBackground = iconBackground,
        onClick = onClick,
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
        },
    )
}
