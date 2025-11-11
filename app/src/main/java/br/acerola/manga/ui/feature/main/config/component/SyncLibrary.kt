package br.acerola.manga.ui.feature.main.config.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import br.acerola.manga.R
import br.acerola.manga.ui.common.component.CardType
import br.acerola.manga.ui.common.component.Divider
import br.acerola.manga.ui.common.component.SmartCard
import br.acerola.manga.ui.common.viewmodel.library.MangaLibraryViewModel

@Composable
fun SyncLibrary(mangaLibraryViewModel: MangaLibraryViewModel) {
    SmartCard(
        type = CardType.CONTENT,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(size = 40.dp)
                        .clip(CircleShape)
                        .background(color = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(size = 22.dp),
                        contentDescription = stringResource(
                            id = R.string.description_icon_sync_manga_folders
                        ),
                    )
                }

                Spacer(modifier = Modifier.width(width = 12.dp))

                Text(
                    text = stringResource(id = R.string.title_config_sync_modal),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Divider()

            ListItem(
                modifier = Modifier.clickable { mangaLibraryViewModel.deepScanLibrary() },
                headlineContent = {
                    Text(text = stringResource(id = R.string.description_text_home_deep_sync))
                },
                supportingContent = {
                    Text(text = stringResource(id = R.string.description_text_home_deep_sync_supporting))
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.SyncLock,
                        contentDescription = null
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )

            HorizontalDivider()

            ListItem(
                modifier = Modifier.clickable { mangaLibraryViewModel.syncLibrary() },
                headlineContent = {
                    Text(text = stringResource(id = R.string.description_text_home_quick_sync))
                },
                supportingContent = {
                    Text(text = stringResource(id = R.string.description_text_home_quick_sync_supporting))
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }
    }
}