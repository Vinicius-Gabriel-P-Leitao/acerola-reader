package br.acerola.comic.module.main.common.component

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import br.acerola.comic.common.ux.Acerola
import br.acerola.comic.common.ux.component.AdaptiveSheet
import br.acerola.comic.common.ux.theme.AcerolaTheme
import br.acerola.comic.common.ux.tokens.SpacingTokens
import br.acerola.comic.module.main.Main
import br.acerola.comic.module.main.sync.state.PairedPeer
import br.acerola.comic.ui.R

/**
 * Bottom sheet that lists the currently paired peers, used by any entry point that fires
 * `SyncComicWithPeerUseCase` (Home's [ComicActionsSheet] and the Comic Detail's config
 * section) — the underlying `acerola/sync-comic/1` session is the same either way, only who
 * asked for it differs.
 */
@Composable
fun Main.Common.Component.PeerPickerSheet(
    peers: List<PairedPeer>,
    onSelect: (peerId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    Acerola.Component.AdaptiveSheet(
        onDismissRequest = onDismiss,
        isScrollable = false,
    ) {
        Text(
            text = stringResource(id = R.string.title_sync_peer_picker),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = SpacingTokens.ExtraLarge, vertical = SpacingTokens.Medium),
        )

        HorizontalDivider()

        if (peers.isEmpty()) {
            ListItem(
                headlineContent = { Text(text = stringResource(id = R.string.label_sync_no_peers)) },
            )
        } else {
            LazyColumn {
                items(items = peers, key = { it.peerId }) { peer ->
                    ListItem(
                        leadingContent = {
                            Icon(imageVector = Icons.Rounded.PhoneAndroid, contentDescription = null)
                        },
                        headlineContent = { Text(text = peer.deviceName ?: peer.peerId) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(peer.peerId) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PeerPickerSheetPreview() {
    AcerolaTheme {
        Main.Common.Component.PeerPickerSheet(
            peers =
                listOf(
                    PairedPeer(peerId = "peer-1", deviceName = "Pixel 8"),
                    PairedPeer(peerId = "peer-2", deviceName = null),
                ),
            onSelect = {},
            onDismiss = {},
        )
    }
}
