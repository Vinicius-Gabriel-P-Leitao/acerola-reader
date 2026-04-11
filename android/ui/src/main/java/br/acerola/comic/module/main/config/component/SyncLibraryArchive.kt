package br.acerola.comic.module.main.config.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.HeroItem
import br.acerola.comic.module.main.Main
import br.acerola.comic.ui.R

@Composable
fun Main.Config.Component.SyncLibraryArchive(
    onDeepScan: () -> Unit,
    onQuickSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Acerola.Component.HeroItem(
            title = stringResource(id = R.string.description_text_home_deep_sync),
            description = stringResource(id = R.string.description_text_home_deep_sync_supporting),
            icon = Icons.Default.Folder,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            iconBackground = MaterialTheme.colorScheme.primaryContainer,
            onClick = onDeepScan
        )
        Spacer(modifier = Modifier.height(8.dp))
        Acerola.Component.HeroItem(
            title = stringResource(id = R.string.description_text_home_quick_sync),
            description = stringResource(id = R.string.description_text_home_quick_sync_supporting),
            icon = Icons.Default.Sync,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            iconBackground = MaterialTheme.colorScheme.primaryContainer,
            onClick = onQuickSync
        )
    }
}
