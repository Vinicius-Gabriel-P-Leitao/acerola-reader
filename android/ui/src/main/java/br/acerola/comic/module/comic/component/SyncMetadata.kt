package br.acerola.comic.module.comic.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.Divider
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
) {
    val syncSource = remoteInfo?.syncSource
    val hasMangadexSource = remoteInfo?.sources?.mangadex?.mangadexId != null
    val hasComicInfoSource = remoteInfo?.sources?.comicInfo?.localHash != null

    Column {
        if (externalSyncEnabled) {
            // NOTE: Mangadex
            MangadexSection(
                isActive = syncSource == MetadataSourcePattern.MANGADEX,
                hasChapters = hasMangadexSource && remoteInfo.id != null,
                onSyncInfo = onSyncMangadexInfo,
                onSyncChapters = onSyncMangadexChapters
            )

            Acerola.Component.Divider(
                modifier = Modifier
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .alpha(0.2f)
            )

            // NOTE: Anilist
            AnilistSection(
                isActive = syncSource == MetadataSourcePattern.ANILIST,
                onSyncInfo = onSyncAnilistInfo
            )

            Acerola.Component.Divider(
                modifier = Modifier
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .alpha(0.2f)
            )
        }

        // NOTE: ComicInfo
        ComicInfoSection(
            isActive = syncSource == MetadataSourcePattern.COMIC_INFO,
            hasChapters = hasComicInfoSource,
            onSyncInfo = onSyncComicInfo,
            onSyncChapters = onSyncComicInfoChapters
        )
    }
}

@Composable
private fun MangadexSection(
    isActive: Boolean,
    hasChapters: Boolean,
    onSyncInfo: () -> Unit,
    onSyncChapters: () -> Unit
) {
    Column {
        SyncItem(
            title = stringResource(id = R.string.label_mangadex_group),
            subtitle = pluralStringResource(
                id = R.plurals.description_sync_mangadex_remote_info_supporting,
                count = 1
            ),
            iconPainter = painterResource(id = R.drawable.mangadex_v2),
            isActive = isActive,
            onClick = onSyncInfo
        )

        if (hasChapters && isActive) {
            Acerola.Component.Divider(
                modifier = Modifier
                    .padding(start = 72.dp, end = 24.dp)
                    .alpha(0.1f)
            )
            SyncItem(
                title = stringResource(id = R.string.title_sync_chapters),
                subtitle = stringResource(id = R.string.description_sync_chapters_remote),
                iconVector = Icons.Rounded.AutoAwesome,
                onClick = onSyncChapters
            )
        }
    }
}

@Composable
private fun AnilistSection(
    isActive: Boolean,
    onSyncInfo: () -> Unit
) {
    SyncItem(
        title = stringResource(id = R.string.title_sync_anilist_remote_info),
        subtitle = stringResource(id = R.string.description_sync_anilist_remote_info),
        iconPainter = painterResource(id = R.drawable.anilist),
        isActive = isActive,
        onClick = onSyncInfo
    )
}

@Composable
private fun ComicInfoSection(
    isActive: Boolean,
    hasChapters: Boolean,
    onSyncInfo: () -> Unit,
    onSyncChapters: () -> Unit
) {
    Column {
        SyncItem(
            title = stringResource(id = R.string.title_sync_comic_info),
            subtitle = stringResource(id = R.string.description_sync_comic_info),
            iconVector = Icons.Rounded.Description,
            isActive = isActive,
            onClick = onSyncInfo
        )

        if (hasChapters && isActive) {
            Acerola.Component.Divider(
                modifier = Modifier
                    .padding(start = 72.dp, end = 24.dp)
                    .alpha(0.1f)
            )
            SyncItem(
                title = stringResource(id = R.string.title_sync_chapters),
                subtitle = stringResource(id = R.string.description_sync_chapters_internal),
                iconVector = Icons.Rounded.AutoStories,
                onClick = onSyncChapters
            )
        }
    }
}

@Composable
private fun SyncItem(
    title: String,
    subtitle: String,
    iconVector: ImageVector? = null,
    iconPainter: Painter? = null,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (iconVector != null) {
                        Icon(
                            imageVector = iconVector,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null
                        )
                    } else if (iconPainter != null) {
                        Image(
                            painter = iconPainter,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        trailingContent = {
            if (isActive) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
