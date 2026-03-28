package br.acerola.manga.service.cache

import br.acerola.manga.error.message.ChapterError
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PageCacheHandlerTest {

    private lateinit var service: PageCacheHandler

    @Before
    fun setUp() {
        service = PageCacheHandler()
    }

    @Test
    fun put_e_get_devem_funcionar_para_dados_validos() {
        val data = byteArrayOf(1, 2, 3)
        service.put(1, data)

        val result = service.get(1)

        assertTrue(result.isRight())
        result.onRight { assertArrayEquals(data, it) }
    }

    @Test
    fun get_deve_retornar_erro_se_pagina_nao_existir() {
        val result = service.get(999)

        assertTrue(result.isLeft())
        result.onLeft { assertTrue(it is ChapterError.UnexpectedError) }
    }

    @Test
    fun clear_deve_remover_todos_os_itens() {
        service.put(1, byteArrayOf(1))
        service.clear()

        assertTrue(service.get(1).isLeft())
    }

    @Test
    fun cache_deve_respeitar_limite_de_tamanho_removendo_antigos() {
        // NOTE: O limite é 60MB.
        val largeData = ByteArray(40 * 1024 * 1024) { 1 } // 40MB
        
        service.put(1, largeData)
        assertTrue("Primeiro item deveria estar no cache", service.get(1).isRight())

        service.put(2, largeData) // Total 80MB > 60MB. O 1 deve ser removido.
        
        assertTrue("Segundo item deveria estar no cache", service.get(2).isRight())
        assertTrue("Primeiro item deveria ter sido removido (LRU)", service.get(1).isLeft())
    }
}
