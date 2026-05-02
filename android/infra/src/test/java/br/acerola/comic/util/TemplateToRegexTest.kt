package br.acerola.comic.util

import br.acerola.comic.pattern.template.TemplateMacro
import br.acerola.comic.util.template.detectArchiveTemplate
import br.acerola.comic.util.template.templateToRegex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateToRegexTest {
    private val chapterTemplates =
        listOf(
            "{${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}.*.{${TemplateMacro.EXTENSION.tag}}",
            "Ch. {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}.*.{${TemplateMacro.EXTENSION.tag}}",
            "Cap. {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}.*.{${TemplateMacro.EXTENSION.tag}}",
            "chapter {${TemplateMacro.CHAPTER.tag}}{${TemplateMacro.DECIMAL.tag}}.*.{${TemplateMacro.EXTENSION.tag}}",
        )

    @Test
    fun `deve converter um padrao de template para regex corretamente`() {
        val template = "Cap. {chapter}{decimal}*{extension}"
        val regex = templateToRegex(template)

        assertTrue(regex.matches("Cap. 01 - Titulo.cbz"))
        assertTrue(regex.matches("Cap. 15.5.cbr"))
        assertFalse(regex.matches("Ch. 01.cbz"))
    }

    @Test
    fun `deve lidar com caracteres especiais escapados na regex`() {
        val template = "[{chapter}] * .{extension}"
        val regex = templateToRegex(template)

        // Deve aceitar qualquer coisa entre os colchetes e a extensao (devido ao *)
        assertTrue(regex.matches("[12] Meu Comic.cbz"))
        assertTrue(regex.matches("[01] - Arquivo Especial.cbr"))
        assertFalse(regex.matches("(01) - Errado.cbz"))
    }

    @Test
    fun `deve evitar erros de sintaxe com variacoes de asterisco e ponto`() {
        // Valida que o erro de .*?? nao ocorre mais
        val template1 = "Ch. {chapter}.*.{extension}"
        val regex1 = templateToRegex(template1)
        assertTrue(regex1.matches("Ch. 10.cbz"))

        val template2 = "Ch. {chapter}.+.{extension}"
        val regex2 = templateToRegex(template2)
        assertTrue(regex2.matches("Ch. 10.cbz"))
    }

    @Test
    fun `detectArchiveTemplate deve identificar corretamente o melhor preset`() {
        val fallback = "{chapter}{decimal}.*.{extension}"

        val preset = detectArchiveTemplate("Cap. 01 - O Início.cbz", chapterTemplates, fallback)
        assertEquals("Cap. {chapter}{decimal}.*.{extension}", preset)

        val preset2 = detectArchiveTemplate("chapter 10.cbz", chapterTemplates, fallback)
        assertEquals("chapter {chapter}{decimal}.*.{extension}", preset2)

        val preset3 = detectArchiveTemplate("Ch. 5.5 - Fim.cbz", chapterTemplates, fallback)
        assertEquals("Ch. {chapter}{decimal}.*.{extension}", preset3)

        val presetFallback = detectArchiveTemplate("FormatoDesconhecido_01.rar", chapterTemplates, fallback)
        assertEquals("{chapter}{decimal}.*.{extension}", presetFallback)
    }
}
