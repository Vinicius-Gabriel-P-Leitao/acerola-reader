package br.acerola.comic.module.main.pattern

import app.cash.turbine.test
import arrow.core.Either
import br.acerola.comic.MainDispatcherRule
import br.acerola.comic.dto.archive.ChapterTemplateDto
import br.acerola.comic.error.message.TemplateError
import br.acerola.comic.module.main.pattern.state.FilePatternAction
import br.acerola.comic.usecase.template.AddTemplateUseCase
import br.acerola.comic.usecase.template.ObserveTemplatesUseCase
import br.acerola.comic.usecase.template.RemoveTemplateUseCase
import br.acerola.comic.usecase.template.UpdateTemplateUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
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
    private val updateTemplate = mockk<UpdateTemplateUseCase>(relaxed = true)
    private val removeTemplate = mockk<RemoveTemplateUseCase>(relaxed = true)
    private val observeTemplates = mockk<ObserveTemplatesUseCase>()

    private val templatesFlow = MutableStateFlow<List<ChapterTemplateDto>>(emptyList())

    private lateinit var viewModel: FilePatternViewModel

    @Before
    fun setup() {
        every { observeTemplates() } returns templatesFlow
        coEvery { addTemplate(any(), any()) } returns Either.Right(Unit)
        coEvery { updateTemplate(any(), any(), any()) } returns Either.Right(Unit)
        viewModel = FilePatternViewModel(addTemplate, updateTemplate, removeTemplate, observeTemplates)
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
            awaitItem()
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

    @Test
    fun `EditTemplate delega para use case com id, label e pattern corretos`() = runTest {
        coEvery { updateTemplate(1L, "Novo Label", "{chapter}") } returns Either.Right(Unit)

        viewModel.onAction(FilePatternAction.EditTemplate(1L, "Novo Label", "{chapter}"))

        coVerify { updateTemplate(1L, "Novo Label", "{chapter}") }
    }

    @Test
    fun `EditTemplate emite evento de erro quando use case retorna Left`() = runTest {
        val error = TemplateError.Duplicate
        coEvery { updateTemplate(any(), any(), any()) } returns Either.Left(error)

        viewModel.uiEvents.test {
            viewModel.onAction(FilePatternAction.EditTemplate(1L, "Label", "{chapter}"))
            val event = awaitItem()
            assertThat(event).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `AddTemplate emite evento de erro quando use case retorna Left`() = runTest {
        coEvery { addTemplate(any(), any()) } returns Either.Left(TemplateError.Duplicate)

        viewModel.uiEvents.test {
            viewModel.onAction(FilePatternAction.AddTemplate("Label", "invalido"))
            val event = awaitItem()
            assertThat(event).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
