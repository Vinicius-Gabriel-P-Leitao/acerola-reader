package br.acerola.comic.service

import android.content.Context
import android.os.Build
import android.util.Log
import p2p.FfiNetworkMode
import p2p.FfiPeerAddr
import p2p.FileSyncProvider
import p2p.HistorySyncProvider
import p2p.P2pCallback
import p2p.P2pNode
import p2p.SecureBlobStore
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

data class ConnectedPeerInfo(
    val peerId: String,
    val alpns: List<ByteArray>,
    val deviceName: String?,
)

class P2pService(
    context: Context,
    relayUrlOverride: String?,
    secureStore: SecureBlobStore,
    historyProvider: HistorySyncProvider,
    fileProvider: FileSyncProvider,
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
        // Only used by Rust to migrate identity/peers/trust from the old plain-text format
        // into `SecureBlobStore` once — see `storage.rs`/`trust_store.rs`.
        val legacyDataDir = context.filesDir.resolve("p2p").absolutePath
        val appVersion =
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: "unknown"
        p2pNode =
            P2pNode(
                callbackHandler,
                legacyDataDir,
                relayUrlOverride,
                Build.MODEL,
                appVersion,
                secureStore,
                historyProvider,
                fileProvider,
            )
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

    fun getConnectedPeersWithInfo(): List<ConnectedPeerInfo> =
        p2pNode.getConnectedPeersWithInfo().map {
            ConnectedPeerInfo(peerId = it.peerId, alpns = it.alpns, deviceName = it.deviceName)
        }

    /**
     * Every peer ever paired (TOFU), with its last known address — persists across app
     * restarts. The handshake connection itself only lasts a few seconds (PING/PONG/DeviceInfo
     * exchange, then closes), so this — not [getConnectedPeers] — is the right source for
     * "paired devices" in the UI.
     */
    fun getPairedPeers(): List<PeerAddress> =
        p2pNode.getPairedPeers().map {
            PeerAddress(id = it.id, deviceId = it.deviceId, addrs = it.addrs)
        }

    fun shutdown() {
        p2pNode.destroy()
    }

    override fun close() {
        shutdown()
    }
}
