package br.acerola.manga.module.manga.component

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
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.common.viewmodel.library.archive.ChapterArchiveViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.dto.archive.MangaDirectoryDto

@Composable
fun SyncMangaArchive(
    directory: MangaDirectoryDto,
    mangaDirectoryViewModel: MangaDirectoryViewModel,
    chapterArchiveViewModel: ChapterArchiveViewModel,
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
                        .background(color = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(size = 22.dp),
                        contentDescription = null,
                    )
                }

                Spacer(modifier = Modifier.width(width = 12.dp))

                Text(
                    text = "Sincronizar arquivos", style = MaterialTheme.typography.titleMedium
                )
            }

            ListItem(
                modifier = Modifier.clickable { chapterArchiveViewModel.syncChaptersByMangaDirectory(folderId = directory.id) },
                headlineContent = { Text(text = "Sincronizar capítulos") },
                supportingContent = { Text(text = "Sincroniza metadados de cada capítulo local") },
                leadingContent = { Icon(imageVector = Icons.Default.Refresh, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text(text = "Sincronizar cover e banner") },
                supportingContent = { Text(text = "Busca imagens já baixadas na pasta") },
                leadingContent = {
                    Icon(imageVector = Icons.Default.ImageSearch, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    mangaDirectoryViewModel.rescanMangaByManga(mangaId = directory.id)
                },
            )
        }
    }
}