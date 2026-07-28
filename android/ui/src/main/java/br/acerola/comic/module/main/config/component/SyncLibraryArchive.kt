package br.acerola.comic.module.main.config.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.HeroButton
import br.acerola.comic.common.ux.tokens.SizeTokens
import br.acerola.comic.module.main.Main
import br.acerola.comic.ui.R

@Composable
fun Main.Config.Component.SyncLibraryArchive(
    onDeepScan: () -> Unit,
    onQuickSync: () -> Unit,
    isDeepScanning: Boolean = false,
    isQuickSyncing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Acerola.Component.HeroButton(
            title = stringResource(id = R.string.description_text_home_deep_sync),
            description = stringResource(id = R.string.description_text_home_deep_sync_supporting),
            iconBackground = MaterialTheme.colorScheme.primaryContainer,
            onClick = if (isDeepScanning || isQuickSyncing) null else onDeepScan,
            icon = {
                AnimatedContent(
                    targetState = isDeepScanning,
                    label = "deepScanLoadingAnimation",
                ) { loading ->
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(SizeTokens.IconMedium),
                        )
                    }
                }
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Acerola.Component.HeroButton(
            title = stringResource(id = R.string.description_text_home_quick_sync),
            description = stringResource(id = R.string.description_text_home_quick_sync_supporting),
            iconBackground = MaterialTheme.colorScheme.primaryContainer,
            onClick = if (isDeepScanning || isQuickSyncing) null else onQuickSync,
            icon = {
                AnimatedContent(
                    targetState = isQuickSyncing,
                    label = "quickSyncLoadingAnimation",
                ) { loading ->
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(SizeTokens.IconMedium),
                        )
                    }
                }
            },
        )
    }
}
