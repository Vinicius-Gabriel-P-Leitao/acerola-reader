package br.acerola.comic.usecase.network

import br.acerola.comic.service.NetworkMode
import br.acerola.comic.service.P2pService
import java.io.Closeable
import javax.inject.Inject

class P2pUseCase @Inject constructor(
    private val p2pService: P2pService
) : Closeable {

    fun getLocalId(): String {
        return p2pService.getLocalId()
    }

    fun connect(
        peerId: String,
        alpn: ByteArray
    ) {
        p2pService.connect(peerId, alpn)
    }

    fun switchToLocal() {
        p2pService.switchToLocal()
    }

    fun switchToRelay() {
        p2pService.switchToRelay()
    }

    fun getMode(): NetworkMode {
        return p2pService.getMode()
    }

    fun getConnectedPeers(): Map<String, List<ByteArray>> {
        return p2pService.getConnectedPeers()
    }

    fun shutdown() {
        p2pService.shutdown()
    }

    override fun close() {
        p2pService.close()
    }
}
