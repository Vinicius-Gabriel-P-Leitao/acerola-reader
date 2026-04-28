package br.acerola.comic.usecase.network

import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import br.acerola.comic.service.NetworkMode
import br.acerola.comic.service.P2pService
import java.io.Closeable
import javax.inject.Inject

class P2pUseCase
    @Inject
    constructor(
        private val p2pService: P2pService,
    ) : Closeable {
        fun getLocalId(): String {
            val id = p2pService.getLocalId()
            AcerolaLogger.d("P2pUseCase", "getLocalId: $id", LogSource.NETWORK)
            return id
        }

        fun connect(
            peerId: String,
            alpn: ByteArray,
        ) {
            AcerolaLogger.i("P2pUseCase", "Connecting to peer: $peerId", LogSource.NETWORK)
            p2pService.connect(peerId, alpn)
        }

        fun switchToLocal() {
            AcerolaLogger.i("P2pUseCase", "Switching to LOCAL mode", LogSource.NETWORK)
            p2pService.switchToLocal()
        }

        fun switchToRelay() {
            AcerolaLogger.i("P2pUseCase", "Switching to RELAY mode", LogSource.NETWORK)
            p2pService.switchToRelay()
        }

        fun getMode(): NetworkMode = p2pService.getMode()

        fun getConnectedPeers(): Map<String, List<ByteArray>> = p2pService.getConnectedPeers()

        fun shutdown() {
            p2pService.shutdown()
        }

        override fun close() {
            p2pService.close()
        }
    }
