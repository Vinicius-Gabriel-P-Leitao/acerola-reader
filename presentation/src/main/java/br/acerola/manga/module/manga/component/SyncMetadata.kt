package br.acerola.manga.module.manga.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.Divider
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.common.viewmodel.library.metadata.ChapterRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.module.manga.MangaViewModel
import br.acerola.manga.presentation.R

import br.acerola.manga.local.database.entity.metadata.MetadataSource

import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Description

@Composable
fun SyncMetadata(
    directory: MangaDirectoryDto,
    remoteInfo: MangaRemoteInfoDto?,
    mangaRemoteInfoViewModel: MangaRemoteInfoViewModel,
    chapterRemoteInfoViewModel: ChapterRemoteInfoViewModel,
) {
    val currentSource = remoteInfo?.metadataSource
    Column {
        Text(
            text = stringResource(id = R.string.label_mangadex_group),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        ListItem(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    mangaRemoteInfoViewModel.syncFromMangadex(directory.id)
                },
            headlineContent = { Text(text = stringResource(id = R.string.title_sync_mangadex_remote_info)) },
            supportingContent = {
                Text(
                    text = pluralStringResource(
                        id = R.plurals.description_sync_mangadex_remote_info_supporting,
                        count = 1
                    )
                )
            },
            leadingContent = {
                Image(
                    painter = painterResource(id = R.drawable.mangadex_v2),
                    contentDescription = null,
                    modifier = Modifier.size(size = 24.dp)
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )

        if (currentSource == MetadataSource.MANGADEX && remoteInfo.id != null) {
            ListItem(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        chapterRemoteInfoViewModel.syncChaptersByMangadex(mangaId = remoteInfo.id!!)
                    },
                headlineContent = { Text(text = stringResource(id = R.string.title_sync_chapters)) },
                supportingContent = { Text(text = stringResource(id = R.string.description_sync_chapters_remote)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .alpha(0.3f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.label_local_file_group),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        ListItem(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    mangaRemoteInfoViewModel.syncFromComicInfo(directory.id)
                },
            headlineContent = { Text(text = stringResource(id = R.string.title_sync_comic_info)) },
            supportingContent = { Text(text = stringResource(id = R.string.description_sync_comic_info)) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )

        if (currentSource == MetadataSource.COMIC_INFO) {
            ListItem(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        chapterRemoteInfoViewModel.syncChaptersByComicInfo(folderId = directory.id)
                    },
                headlineContent = { Text(text = stringResource(id = R.string.title_sync_chapters)) },
                supportingContent = { Text(text = stringResource(id = R.string.description_sync_chapters_internal)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.AutoStories,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}