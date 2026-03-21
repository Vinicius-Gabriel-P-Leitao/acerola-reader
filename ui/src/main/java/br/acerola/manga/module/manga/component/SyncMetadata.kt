package br.acerola.manga.module.manga.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.ux.Acerola
import br.acerola.manga.common.ux.component.Divider
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.module.manga.Manga
import br.acerola.manga.pattern.MetadataSource
import br.acerola.manga.ui.R

import androidx.compose.foundation.shape.CircleShape

@Composable
fun Manga.Component.SyncMetadata(
    remoteInfo: MangaRemoteInfoDto?,
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
        ListItem(
            modifier = Modifier.clickable { onSyncMangadexInfo() },
            headlineContent = { 
                Text(
                    text = stringResource(id = R.string.title_sync_mangadex_remote_info),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                ) 
            },
            supportingContent = {
                Text(
                    text = pluralStringResource(
                        id = R.plurals.description_sync_mangadex_remote_info_supporting,
                        count = 1
                    ),
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
                        Image(
                            painter = painterResource(id = R.drawable.mangadex_v2),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            trailingContent = {
                if (syncSource == MetadataSource.MANGADEX) {
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

        if (hasMangadexSource && remoteInfo?.id != null && syncSource == MetadataSource.MANGADEX) {
            ListItem(
                modifier = Modifier.clickable { onSyncMangadexChapters() },
                headlineContent = { 
                    Text(
                        text = stringResource(id = R.string.title_sync_chapters),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                supportingContent = { 
                    Text(
                        text = stringResource(id = R.string.description_sync_chapters_remote),
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
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        ListItem(
            modifier = Modifier.clickable { onSyncAnilistInfo() },
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.title_sync_anilist_remote_info),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(id = R.string.description_sync_anilist_remote_info),
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
                        Image(
                            painter = painterResource(id = R.drawable.anilist),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            trailingContent = {
                if (syncSource == MetadataSource.ANILIST) {
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

        ListItem(
            modifier = Modifier.clickable { onSyncComicInfo() },
            headlineContent = { 
                Text(
                    text = stringResource(id = R.string.title_sync_comic_info),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                ) 
            },
            supportingContent = { 
                Text(
                    text = stringResource(id = R.string.description_sync_comic_info),
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
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null
                        )
                    }
                }
            },
            trailingContent = {
                if (syncSource == MetadataSource.COMIC_INFO) {
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

        if (hasComicInfoSource && syncSource == MetadataSource.COMIC_INFO) {
            ListItem(
                modifier = Modifier.clickable { onSyncComicInfoChapters() },
                headlineContent = { 
                    Text(
                        text = stringResource(id = R.string.title_sync_chapters),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                supportingContent = { 
                    Text(
                        text = stringResource(id = R.string.description_sync_chapters_internal),
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
                            Icon(
                                imageVector = Icons.Rounded.AutoStories,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}
