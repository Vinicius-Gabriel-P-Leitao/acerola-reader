package br.acerola.comic.service.network

/**
 * Typed representation of the raw `(event, data)` events emitted by the Rust P2P node.
 * `data` arrives as a string (usually JSON) whose format depends on the event itself.
 */
sealed interface P2pEvent {
    data class PeerTrustedFirstTime(
        val peerId: String,
    ) : P2pEvent

    /**
     * The handshake (`acerola/handshake/1`) finished exchanging identity/DeviceInfo with this
     * peer — a deterministic signal that "pairing worked", emitted by the library itself
     * (`rpc:device_info_received` on the side that connected, `rpc:device_info_exchanged` on
     * the side that received). Far more reliable than polling the peer list with a timeout.
     */
    data class HandshakeCompleted(
        val peerId: String,
    ) : P2pEvent

    data class HistorySyncStarted(
        val peerId: String,
    ) : P2pEvent

    data class HistorySyncComplete(
        val peerId: String,
        val progressApplied: Int,
        val progressSkipped: Int,
        val chaptersReadApplied: Int,
        val chaptersReadSkipped: Int,
    ) : P2pEvent

    data class HistorySyncError(
        val peerId: String,
        val message: String,
    ) : P2pEvent

    data class FileSyncManifestExchanged(
        val peerId: String,
        val missingCount: Int,
        val offeringCount: Int,
    ) : P2pEvent

    data class FileSyncProgress(
        val peerId: String,
        val comicName: String,
        val chapter: String,
        val bytesTransferred: Long,
        val totalBytes: Long,
    ) : P2pEvent

    data class FileSyncChapterComplete(
        val peerId: String,
        val comicName: String,
        val chapter: String,
    ) : P2pEvent

    data class FileSyncChapterFailed(
        val peerId: String,
        val comicName: String,
        val chapter: String,
        val reason: String,
    ) : P2pEvent

    data class FileSyncComplete(
        val peerId: String,
        val receivedCount: Int,
        val sentCount: Int,
        val failedCount: Int,
    ) : P2pEvent

    data class Unknown(
        val event: String,
        val data: String,
    ) : P2pEvent
}
