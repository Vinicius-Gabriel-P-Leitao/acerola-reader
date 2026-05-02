package br.acerola.comic.common.viewmodel.network

import br.acerola.comic.service.NetworkMode
import br.acerola.comic.usecase.network.P2pUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class P2pViewModelTest {
    private val p2pUseCase = mockk<P2pUseCase>(relaxed = true)
    private lateinit var viewModel: P2pViewModel

    @Before
    fun setup() {
        viewModel = P2pViewModel(p2pUseCase)
    }

    @Test
    fun `deve retornar id local do use case`() {
        every { p2pUseCase.getLocalId() } returns "my-id"
        assertThat(viewModel.getLocalId()).isEqualTo("my-id")
    }

    @Test
    fun `deve delegar conexao para o use case`() {
        val alpn = "test".toByteArray()
        viewModel.connectToPeer("peer-1", alpn)
        verify { p2pUseCase.connect("peer-1", alpn) }
    }

    @Test
    fun `deve alternar para modo local`() {
        viewModel.switchToLocal()
        verify { p2pUseCase.switchToLocal() }
    }

    @Test
    fun `deve alternar para modo relay`() {
        viewModel.switchToRelay()
        verify { p2pUseCase.switchToRelay() }
    }

    @Test
    fun `deve retornar modo atual`() {
        every { p2pUseCase.getMode() } returns NetworkMode.LOCAL
        assertThat(viewModel.getMode()).isEqualTo(NetworkMode.LOCAL)
    }
}
