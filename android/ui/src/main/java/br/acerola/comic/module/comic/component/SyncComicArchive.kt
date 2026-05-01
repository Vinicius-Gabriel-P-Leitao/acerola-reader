package br.acerola.comic.module.comic.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.HeroItem
import br.acerola.comic.module.comic.Comic
import br.acerola.comic.ui.R

@Composable
fun Comic.Component.SyncMangaArchive(
    onSyncChapters: () -> Unit,
    onRescanCover: () -> Unit,
    onExtractFirstPageAsCover: () -> Unit,
    onExtractVolumeCovers: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Acerola.Component.HeroItem(
            title = stringResource(id = R.string.title_sync_chapters),
            description = stringResource(id = R.string.description_sync_chapters_local),
            icon = Icons.Default.SyncAlt,
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            iconBackground = MaterialTheme.colorScheme.secondaryContainer,
            onClick = onSyncChapters,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Acerola.Component.HeroItem(
            title = stringResource(id = R.string.title_sync_cover_banner),
            description = stringResource(id = R.string.description_sync_cover_banner),
            icon = Icons.Default.Collections,
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            iconBackground = MaterialTheme.colorScheme.secondaryContainer,
            onClick = onRescanCover,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Acerola.Component.HeroItem(
            title = stringResource(id = R.string.title_extract_first_page_as_cover),
            description = stringResource(id = R.string.description_extract_first_page_as_cover),
            icon = Icons.Default.ImageSearch,
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            iconBackground = MaterialTheme.colorScheme.secondaryContainer,
            onClick = onExtractFirstPageAsCover,
        )

        if (onExtractVolumeCovers != null) {
            Spacer(modifier = Modifier.height(8.dp))

            Acerola.Component.HeroItem(
                title = stringResource(id = R.string.title_extract_volume_covers),
                description = stringResource(id = R.string.description_extract_volume_covers),
                icon = Icons.Default.AutoAwesomeMotion,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                onClick = onExtractVolumeCovers,
            )
        }
    }
}
