package br.acerola.comic.module.main.config.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.HeroButton
import br.acerola.comic.module.main.Main
import br.acerola.comic.ui.R

@Composable
fun Main.Config.Component.SyncAnilistData(
    onRescan: () -> Unit,
    isSyncing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Acerola.Component.HeroButton(
        title = stringResource(id = R.string.title_sync_anilist_remote_info),
        description = stringResource(id = R.string.description_sync_anilist_remote_info),
        iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
        onClick = if (isSyncing) null else onRescan,
        modifier = modifier,
        icon = {
            AnimatedContent(
                targetState = isSyncing,
                label = "anilistSyncLoadingAnimation",
            ) { loading ->
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.anilist),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        },
    )
}
