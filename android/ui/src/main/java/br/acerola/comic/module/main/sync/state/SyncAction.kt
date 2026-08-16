package br.acerola.comic.module.main.sync.state

sealed interface SyncAction {
    /** Decodes a code (pasted or scanned) and proposes pairing for confirmation. */
    data class ProposeConnect(
        val code: String,
    ) : SyncAction

    data object ConfirmConnect : SyncAction

    data object CancelConnect : SyncAction

    data class SyncHistory(
        val peerId: String,
    ) : SyncAction

    data class SyncFiles(
        val peerId: String,
    ) : SyncAction

    data class SyncAll(
        val peerId: String,
    ) : SyncAction

    data object DismissTrustDialog : SyncAction

    data object DismissConnectError : SyncAction
}
