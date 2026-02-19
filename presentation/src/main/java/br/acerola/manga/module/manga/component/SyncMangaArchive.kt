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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.component.CardType
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.common.viewmodel.library.archive.ChapterArchiveViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.presentation.R

@Composable
fun SyncMangaArchive(
    directory: MangaDirectoryDto,
    mangaDirectoryViewModel: MangaDirectoryViewModel,
    chapterArchiveViewModel: ChapterArchiveViewModel,
) {
    Column {
        ListItem(
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                chapterArchiveViewModel.syncChaptersByMangaDirectory(folderId = directory.id) 
            },
            headlineContent = { Text(text = stringResource(id = R.string.title_sync_chapters)) },
            supportingContent = { Text(text = stringResource(id = R.string.description_sync_chapters_local)) },
            leadingContent = { 
                Icon(
                    imageVector = Icons.Default.Refresh, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                ) 
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        ListItem(
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                mangaDirectoryViewModel.rescanMangaByManga(mangaId = directory.id)
            },
            headlineContent = { Text(text = stringResource(id = R.string.title_sync_cover_banner)) },
            supportingContent = { Text(text = stringResource(id = R.string.description_sync_cover_banner)) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.ImageSearch, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}