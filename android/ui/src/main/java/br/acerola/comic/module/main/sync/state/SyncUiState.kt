package br.acerola.comic.module.main.sync.state

import br.acerola.comic.service.NetworkMode

data class SyncUiState(
    val localId: String = "",
    val localDeviceName: String = "",
    val pairingCode: String? = null,
    val mode: NetworkMode = NetworkMode.LOCAL,
    val relayUrl: String = "",
    val isRelayOverridden: Boolean = false,
    val pairedPeers: List<PairedPeer> = emptyList(),
    val pendingConnect: PendingConnect? = null,
    val connecting: Boolean = false,
    val connectError: ConnectError? = null,
    val trustedPeerDialogPeerId: String? = null,
    val transferLog: List<TransferLogEntry> = emptyList(),
    /**
     * `"$peerId:$kind"` keys (kind = "history"/"files") with a sync session in flight —
     * disables that peer/protocol's buttons to avoid firing a second concurrent session for
     * the same (peer, ALPN) pair. Granularity is per kind, not just per peer: "Sync All"
     * fires history AND files for the same peer, and both need to run at once without
     * blocking each other.
     */
    val syncingKeys: Set<String> = emptySet(),
    /** `peerId` -> timestamp (epoch ms) of the last successfully completed session. */
    val lastSyncedByPeer: Map<String, Long> = emptyMap(),
)

data class PairedPeer(
    val peerId: String,
    val deviceName: String?,
)

data class PendingConnect(
    val peerId: String,
    val deviceId: String?,
    val addrs: ByteArray,
)

enum class ConnectError {
    INVALID_CODE,
    CONNECTION_FAILED,
}

enum class LogState {
    IN_PROGRESS,
    SUCCESS,
    ERROR,
}

/**
 * Raw data for one activity-log row — deliberately no pre-rendered display string here.
 * [br.acerola.comic.module.main.sync.SyncScreen] is the one that turns this into text via
 * `stringResource`, same as it already does for [ConnectError] in `LaunchedEffect`. Keeps
 * string-resource resolution (and thus any [android.content.Context] dependency) out of the
 * ViewModel entirely.
 */
data class TransferLogEntry(
    val id: Long,
    /** `"history"` or `"files"`. */
    val kind: String,
    /** `"started"`, `"progress"`, `"complete"`, `"error"`, or `"chapterFailed"` (files only). */
    val status: String,
    val state: LogState,
    val comicName: String? = null,
    val chapter: String? = null,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
