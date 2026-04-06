package br.acerola.comic.module.main.pattern

import app.cash.turbine.test
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.usecase.template.AddTemplateUseCase
import br.acerola.comic.usecase.template.ObserveTemplatesUseCase
import br.acerola.comic.usecase.template.RemoveTemplateUseCase
import br.acerola.comic.dto.archive.ChapterTemplateDto
import br.acerola.comic.module.main.pattern.state.FilePatternAction
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilePatternViewModelTest {

    @get:Rule
    val coroutineRule = MainDispatcherRule()

    private val addTemplate = mockk<AddTemplateUseCase>(relaxed = true)
    private val removeTemplate = mockk<RemoveTemplateUseCase>(relaxed = true)
    private val observeTemplates = mockk<ObserveTemplatesUseCase>()

    private val templatesFlow = MutableStateFlow<List<ChapterTemplateDto>>(emptyList())

    private lateinit var viewModel: FilePatternViewModel

    @Before
    fun setup() {
        every { observeTemplates() } returns templatesFlow
        viewModel = FilePatternViewModel(addTemplate, removeTemplate, observeTemplates)
    }

    @Test
    fun `estado inicial tem lista de templates vazia`() = runTest {
        viewModel.uiState.test {
            assertThat(awaitItem().templates).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `estado e atualizado quando use case emite novos templates`() = runTest {
        val template = ChapterTemplateDto(id = 1L, label = "Vol. Cap.", pattern = "{chapter}")

        viewModel.uiState.test {
            awaitItem() // estado inicial vazio
            templatesFlow.value = listOf(template)
            assertThat(awaitItem().templates).containsExactly(template)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `AddTemplate delega para use case com label e pattern corretos`() = runTest {
        viewModel.onAction(FilePatternAction.AddTemplate("Vol.", "{chapter}"))

        coVerify { addTemplate("Vol.", "{chapter}") }
    }

    @Test
    fun `DeleteTemplate delega para use case com id correto`() = runTest {
        viewModel.onAction(FilePatternAction.DeleteTemplate(42L))

        coVerify { removeTemplate(42L) }
    }
}
