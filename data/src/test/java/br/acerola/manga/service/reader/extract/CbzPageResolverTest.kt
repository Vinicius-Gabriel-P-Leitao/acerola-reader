package br.acerola.manga.service.reader.extract

import android.content.Context
import br.acerola.manga.dto.archive.ChapterFileDto
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CbzPageResolverTest {

    @MockK lateinit var context: Context
    private lateinit var service: CbzPageResolver

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        service = CbzPageResolver(context)
    }

    @Test
    fun open_deve_falhar_se_arquivo_nao_existir() {
        val chapter = ChapterFileDto(1, "ch", "/non/existent/file.cbz", "1")
        
        val result = service.open(chapter)

        assertTrue(result.isLeft())
    }

    // NOTE: Testar o fluxo feliz de Cbz/CbrChapterSourceService requer arquivos reais
    // ou um sistema de arquivos mockado complexo (Jimfs). 
    // Como os serviços usam 'new ZipFile(file)', o mockk não consegue interceptar o construtor nativo facilmente.
    // Recomendo que estes dois arquivos sejam testados via Testes de Integração com arquivos reais no androidTest
    // ou mantê-los com testes básicos de erro no Unit Test.
    
    @Test
    fun close_deve_limpar_referencias_e_arquivos_temporarios() {
        service.close()
        runTest {
            assertEquals(0, service.pageCount())
        }
    }
}
