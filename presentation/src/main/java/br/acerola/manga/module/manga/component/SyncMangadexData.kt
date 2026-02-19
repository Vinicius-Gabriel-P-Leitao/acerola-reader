package br.acerola.manga.module.manga.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.common.viewmodel.library.metadata.ChapterRemoteInfoViewModel
import br.acerola.manga.common.viewmodel.library.metadata.MangaRemoteInfoViewModel
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.presentation.R

@Composable
fun SyncMangadexData(
    directory: br.acerola.manga.dto.archive.MangaDirectoryDto,
    remoteInfo: MangaRemoteInfoDto?,
    mangaViewModel: br.acerola.manga.module.manga.MangaViewModel,
    mangaRemoteInfoViewModel: MangaRemoteInfoViewModel,
    chapterRemoteInfoViewModel: ChapterRemoteInfoViewModel,
) {
    SmartCard(
        type = CardType.CONTENT,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(size = 40.dp)
                        .clip(CircleShape)
                        .background(color = MaterialTheme.colorScheme.onSurface)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mangadex_v2),
                        contentDescription = stringResource(id = R.string.description_icon_sync_mangadex),
                        modifier = Modifier.size(size = 30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(width = 12.dp))

                Text(
                    text = stringResource(id = R.string.title_config_sync_mangadex),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { mangaViewModel.syncFromMangadex(directory.id, directory.name) },
                headlineContent = { Text(text = stringResource(id = R.string.title_sync_mangadex_remote_info)) },
                leadingContent = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null) },
                supportingContent = {
                    Text(
                        text = pluralStringResource(
                            id = R.plurals.description_sync_mangadex_remote_info_supporting,
                            count = 1
                        )
                    )
                },
            )

            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { 
                    mangaViewModel.syncFromComicInfo(directory.id, directory.name, directory.path)
                },
                headlineContent = { Text(text = "Sincronizar ComicInfo.xml") },
                supportingContent = { Text(text = "Busca metadados locais no arquivo ou pasta") },
                leadingContent = { Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null) },
            )

            if (remoteInfo != null) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { chapterRemoteInfoViewModel.syncChaptersByManga(mangaId = remoteInfo.id!!) },
                    headlineContent = { Text(text = stringResource(id = R.string.title_sync_chapters)) },
                    supportingContent = { Text(text = stringResource(id = R.string.description_sync_chapters_remote)) },
                    leadingContent = { Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null) },
                )
            }
        }
    }
}