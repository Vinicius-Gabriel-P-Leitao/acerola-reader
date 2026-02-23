package br.acerola.manga.module.config.component

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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncLock
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import br.acerola.manga.common.component.Divider
import br.acerola.manga.common.component.SmartCard
import br.acerola.manga.common.viewmodel.library.archive.MangaDirectoryViewModel
import br.acerola.manga.presentation.R

@Composable
fun SyncLibraryArchive(
    mangaDirectoryViewModel: MangaDirectoryViewModel,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // TODO: Criar description
        ListItem(
            modifier = Modifier.clickable { mangaDirectoryViewModel.deepScanLibrary() },
            headlineContent = { Text(text = stringResource(id = R.string.description_text_home_deep_sync)) },
            supportingContent = { Text(text = stringResource(id = R.string.description_text_home_deep_sync_supporting)) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Folder, contentDescription = null
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // TODO: Criar description
        ListItem(
            modifier = Modifier.clickable { mangaDirectoryViewModel.rescanMangas() },
            headlineContent = {
                Text(text = stringResource(id = R.string.description_text_home_quick_sync))
            },
            supportingContent = {
                Text(text = stringResource(id = R.string.description_text_home_quick_sync_supporting))
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Sync, contentDescription = null
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}