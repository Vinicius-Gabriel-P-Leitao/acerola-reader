package br.acerola.comic.module.main.config.component

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.HeroItem
import br.acerola.comic.module.main.Main
import br.acerola.comic.ui.R

@Composable
fun Main.Config.Component.SyncAnilistData(
    onRescan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Acerola.Component.HeroItem(
        title = stringResource(id = R.string.title_sync_anilist_remote_info),
        description = stringResource(id = R.string.description_sync_anilist_remote_info),
        iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
        onClick = onRescan,
        modifier = modifier,
        icon = {
            Image(
                painter = painterResource(id = R.drawable.anilist),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }
    )
}
