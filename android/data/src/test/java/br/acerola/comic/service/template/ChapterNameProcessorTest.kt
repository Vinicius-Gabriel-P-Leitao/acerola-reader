package br.acerola.comic.service.template

import android.database.sqlite.SQLiteConstraintException
import br.acerola.comic.error.message.TemplateError
import br.acerola.comic.infra.R
import br.acerola.comic.local.dao.archive.ArchiveTemplateDao
import br.acerola.comic.local.entity.archive.ArchiveTemplate
import br.acerola.comic.util.sort.SortType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChapterNameProcessorTest {
    @MockK
    lateinit var dao: ArchiveTemplateDao

    private lateinit var service: ChapterNameProcessor

    private val customTemplate =
        ArchiveTemplate(
            id = 10L,
            label = "Template Existente",
            pattern = "{chapter}*{extension}",
            type = SortType.CHAPTER,
            isDefault = false,
            priority = 1,
        )

    private val defaultTemplate =
        ArchiveTemplate(
            id = 1L,
            label = "Padrão do Sistema",
            pattern = "{chapter}*{extension}",
            type = SortType.CHAPTER,
            isDefault = true,
            priority = 0,
        )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        service = ChapterNameProcessor(dao)

        coEvery { dao.insert(any()) } returns 1L
        coEvery { dao.update(any()) } returns Unit
    }

    // region addTemplate

    @Test
    fun `deve anexar a extensao automaticamente se o usuario nao a prover`() =
        runTest {
            val result = service.addTemplate("Meu Template", "Cap. {chapter}", SortType.CHAPTER)

            assertTrue(result.isRight())
        }

    @Test
    fun `deve remover lixo apos a extensao caso o usuario forneca`() =
        runTest {
            val result = service.addTemplate("Template com Lixo", "Ch. {chapter}{extension} LixoAqui", SortType.CHAPTER)

            assertTrue(result.isRight())
        }

    @Test
    fun `deve rejeitar padrao invalido com erros apropriados`() =
        runTest {
            val result = service.addTemplate("Template Invalido", "Sem a macro de valor", SortType.CHAPTER)

            assertTrue(result.isLeft())
            result.onLeft {
                assertTrue(it is TemplateError.InvalidPattern)
                assertEquals(R.string.error_template_chapter_required, (it as TemplateError.InvalidPattern).uiMessage.resId)
            }
        }

    @Test
    fun `addTemplate retorna Duplicate quando dao retorna -1`() =
        runTest {
            coEvery { dao.insert(any()) } returns -1L

            val result = service.addTemplate("Duplicado", "{chapter}", SortType.CHAPTER)

            assertTrue(result.isLeft())
            result.onLeft { assertTrue(it is TemplateError.Duplicate) }
        }

    // endregion

    // region updateTemplate

    @Test
    fun `updateTemplate tem sucesso com template customizado valido`() =
        runTest {
            coEvery { dao.getTemplateById(10L) } returns customTemplate

            val result = service.updateTemplate(10L, "Novo Label", "{chapter}", SortType.CHAPTER)

            assertTrue(result.isRight())
            coVerify { dao.update(any()) }
        }

    @Test
    fun `updateTemplate retorna SystemProtected quando template nao e encontrado`() =
        runTest {
            coEvery { dao.getTemplateById(99L) } returns null

            val result = service.updateTemplate(99L, "Label", "{chapter}", SortType.CHAPTER)

            assertTrue(result.isLeft())
            result.onLeft { assertTrue(it is TemplateError.SystemProtected) }
        }

    @Test
    fun `updateTemplate retorna SystemProtected ao tentar editar template padrao`() =
        runTest {
            coEvery { dao.getTemplateById(1L) } returns defaultTemplate

            val result = service.updateTemplate(1L, "Novo Label", "{chapter}", SortType.CHAPTER)

            assertTrue(result.isLeft())
            result.onLeft { assertTrue(it is TemplateError.SystemProtected) }
        }

    @Test
    fun `updateTemplate retorna Duplicate quando dao lanca SQLiteConstraintException`() =
        runTest {
            coEvery { dao.getTemplateById(10L) } returns customTemplate
            coEvery { dao.update(any()) } throws SQLiteConstraintException("UNIQUE constraint failed")

            val result = service.updateTemplate(10L, "Label Duplicado", "{chapter}", SortType.CHAPTER)

            assertTrue(result.isLeft())
            result.onLeft { assertTrue(it is TemplateError.Duplicate) }
        }

    @Test
    fun `updateTemplate rejeita padrao sem macro de capitulo`() =
        runTest {
            coEvery { dao.getTemplateById(10L) } returns customTemplate

            val result = service.updateTemplate(10L, "Label", "sem macro nenhuma", SortType.CHAPTER)

            assertTrue(result.isLeft())
            result.onLeft { assertTrue(it is TemplateError.InvalidPattern) }
        }

    @Test
    fun `updateTemplate adiciona extensao automaticamente se nao fornecida`() =
        runTest {
            coEvery { dao.getTemplateById(10L) } returns customTemplate

            val result = service.updateTemplate(10L, "Label", "Cap. {chapter}", SortType.CHAPTER)

            assertTrue(result.isRight())
        }

    @Test
    fun `updateTemplate remove lixo apos a extensao`() =
        runTest {
            coEvery { dao.getTemplateById(10L) } returns customTemplate

            val result = service.updateTemplate(10L, "Label", "Ch. {chapter}{extension} lixo", SortType.CHAPTER)

            assertTrue(result.isRight())
        }

    // endregion
}
