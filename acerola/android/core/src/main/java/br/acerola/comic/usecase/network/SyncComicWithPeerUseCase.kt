package br.acerola.comic.usecase.network

import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import javax.inject.Inject

/**
 * Resolves a paired peer's address by id and fires an `acerola/sync-comic/1` session for a
 * single comic — shared by both the push entry point (user picks a comic they already have)
 * and the pull entry point (user picks a comic discovered by browsing a peer's library), since
 * the underlying exchange is symmetric either way.
 */
class SyncComicWithPeerUseCase
    @Inject
    constructor(
        private val p2pUseCase: P2pUseCase,
    ) {
        /** Returns `false` if `peerId` isn't currently paired (nothing was fired). */
        operator fun invoke(
            peerId: String,
            comicName: String,
        ): Boolean {
            val peerAddress = p2pUseCase.getPairedPeers().find { it.id == peerId }
            if (peerAddress == null) {
                AcerolaLogger.w("SyncComicWithPeerUseCase", "Peer not paired: $peerId", LogSource.NETWORK)
                return false
            }

            p2pUseCase.syncComic(peerAddress, comicName)
            return true
        }
    }
