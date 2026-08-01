package br.acerola.comic.service

import android.util.Log
import p2p.FfiNetworkMode
import p2p.FfiPeerAddr
import p2p.P2pCallback
import p2p.P2pNode
import java.io.Closeable

enum class NetworkMode {
    LOCAL,
    RELAY,
}

data class PeerAddress(
    val id: String,
    val deviceId: String?,
    val addrs: ByteArray,
)

class P2pService(
    private val eventListener: (event: String, data: String) -> Unit,
) : Closeable {
    private val p2pNode: P2pNode

    private val callbackHandler =
        object : P2pCallback {
            override fun onEvent(
                event: String,
                data: String,
            ) {
                Log.d("P2pService", "Event received: $event, Data: $data")
                eventListener(event, data)
            }
        }

    init {
        p2pNode = P2pNode(callbackHandler)
    }

    fun getLocalId(): String = p2pNode.getLocalId()

    fun getLocalAddress(): PeerAddress {
        val addr = p2pNode.getLocalAddr()
        return PeerAddress(
            id = addr.id,
            deviceId = addr.deviceId,
            addrs = addr.addrs,
        )
    }

    fun connect(
        peerAddress: PeerAddress,
        alpn: ByteArray,
    ) {
        Log.d("P2pService", "Connecting to peer: ${peerAddress.id}")
        val ffiAddr =
            FfiPeerAddr(
                id = peerAddress.id,
                deviceId = peerAddress.deviceId,
                addrs = peerAddress.addrs,
            )
        p2pNode.connect(ffiAddr, alpn)
    }

    fun switchToLocal() {
        p2pNode.switchToLocal()
    }

    fun switchToRelay() {
        p2pNode.switchToRelay()
    }

    fun getMode(): NetworkMode =
        when (p2pNode.getMode()) {
            FfiNetworkMode.LOCAL -> NetworkMode.LOCAL
            FfiNetworkMode.RELAY -> NetworkMode.RELAY
        }

    fun getConnectedPeers(): Map<String, List<ByteArray>> = p2pNode.getConnectedPeers()

    fun shutdown() {
        p2pNode.destroy()
    }

    override fun close() {
        shutdown()
    }
}
