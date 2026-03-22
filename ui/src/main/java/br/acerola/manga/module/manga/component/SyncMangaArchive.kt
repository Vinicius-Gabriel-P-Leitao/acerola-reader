package br.acerola.manga.module.manga.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.acerola.manga.common.viewmodel.library.archive.ChapterArchiveViewModel
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.module.manga.Manga
import br.acerola.manga.ui.R

import androidx.compose.foundation.shape.CircleShape

@Composable
fun Manga.Component.SyncMangaArchive(
    onSyncChapters: () -> Unit,
    onRescanCover: () -> Unit,
) {
    Column {
        ListItem(
            modifier = Modifier.clickable { onSyncChapters() },
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.title_sync_chapters),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(id = R.string.description_sync_chapters_local),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            leadingContent = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                            contentDescription = null
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        ListItem(
            modifier = Modifier.clickable { onRescanCover() },
            headlineContent = {
                Text(
                    text = stringResource(id = R.string.title_sync_cover_banner),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(id = R.string.description_sync_cover_banner),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            leadingContent = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ImageSearch,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                            contentDescription = null
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
