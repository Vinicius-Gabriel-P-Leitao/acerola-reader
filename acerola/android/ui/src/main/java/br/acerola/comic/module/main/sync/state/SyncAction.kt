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

    data class RemovePeer(
        val peerId: String,
    ) : SyncAction

    /** Pede a lista de quadrinhos do peer (nome + contagem de capítulos), sem sincronizar
     *  nada — abre o [br.acerola.comic.module.main.sync.RemoteLibrarySheet]. */
    data class BrowseLibrary(
        val peerId: String,
    ) : SyncAction

    data object DismissLibraryBrowse : SyncAction

    /** Sincroniza um único quadrinho (descoberto via [BrowseLibrary]) com o peer. */
    data class SyncComic(
        val peerId: String,
        val comicName: String,
    ) : SyncAction
}
