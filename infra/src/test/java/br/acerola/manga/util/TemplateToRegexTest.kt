package br.acerola.manga.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateToRegexTest {

    @Test
    fun `deve converter um padrao de template para regex corretamente`() {
        val template = "Cap. {value}{sub}*{extension}"
        val regex = templateToRegex(template)

        assertTrue(regex.matches("Cap. 01 - Titulo.cbz"))
        assertTrue(regex.matches("Cap. 15.5.cbr"))
        assertFalse(regex.matches("Ch. 01.cbz"))
    }

    @Test
    fun `deve lidar com caracteres especiais escapados na regex`() {
        val template = "[{value}] * .{extension}"
        val regex = templateToRegex(template)

        // Deve aceitar qualquer coisa entre os colchetes e a extensao (devido ao *)
        assertTrue(regex.matches("[12] Meu Manga.cbz"))
        assertTrue(regex.matches("[01] - Arquivo Especial.cbr"))
        assertFalse(regex.matches("(01) - Errado.cbz"))
    }

    @Test
    fun `deve evitar erros de sintaxe com variacoes de asterisco e ponto`() {
        // Valida que o erro de .*?? nao ocorre mais
        val template1 = "Ch. {value}.*.{extension}"
        val regex1 = templateToRegex(template1)
        assertTrue(regex1.matches("Ch. 10.cbz"))

        val template2 = "Ch. {value}.+.{extension}"
        val regex2 = templateToRegex(template2)
        assertTrue(regex2.matches("Ch. 10.cbz"))
    }

    @Test
    fun `detectTemplate deve identificar corretamente o melhor preset`() {
        val preset = detectTemplate("Cap. 01 - O Início.cbz")
        assertEquals("Cap. {value}{sub}.*.{extension}", preset)

        val preset2 = detectTemplate("chapter 10.cbz")
        assertEquals("chapter {value}{sub}.*.{extension}", preset2)

        val preset3 = detectTemplate("Ch. 5.5 - Fim.cbz")
        assertEquals("Ch. {value}{sub}.*.{extension}", preset3)
        
        val presetFallback = detectTemplate("FormatoDesconhecido_01.rar")
        assertEquals("Ch. {value}{sub}.*.{extension}", presetFallback)
    }
}