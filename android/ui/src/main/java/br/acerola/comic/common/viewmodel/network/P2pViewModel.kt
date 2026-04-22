package br.acerola.comic.common.viewmodel.network

import androidx.lifecycle.ViewModel
import br.acerola.comic.service.NetworkMode
import br.acerola.comic.usecase.network.P2pUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class P2pViewModel @Inject constructor(
    private val p2pUseCase: P2pUseCase
) : ViewModel() {

    fun getLocalId() = p2pUseCase.getLocalId()

    fun connectToPeer(
        peerId: String,
        alpn: ByteArray
    ) {
        p2pUseCase.connect(peerId, alpn)
    }

    fun switchToLocal() {
        p2pUseCase.switchToLocal()
    }

    fun switchToRelay() {
        p2pUseCase.switchToRelay()
    }

    fun getMode(): NetworkMode = p2pUseCase.getMode()

    fun getConnectedPeers() = p2pUseCase.getConnectedPeers()

    override fun onCleared() {
        super.onCleared()
        p2pUseCase.close()
    }
}
