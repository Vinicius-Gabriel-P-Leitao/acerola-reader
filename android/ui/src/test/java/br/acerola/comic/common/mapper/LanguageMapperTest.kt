package br.acerola.comic.common.mapper

import br.acerola.comic.ui.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LanguageMapperTest {
    @Test
    fun `deve retornar o recurso de string correto para cada codigo de idioma`() {
        assertThat(LanguageMapper.getLabelRes("pt-br")).isEqualTo(R.string.lang_pt_br)
        assertThat(LanguageMapper.getLabelRes("en")).isEqualTo(R.string.lang_en)
        assertThat(LanguageMapper.getLabelRes("ja")).isEqualTo(R.string.lang_ja)
    }

    @Test
    fun `deve retornar desconhecido quando o codigo de idioma for invalido ou nulo`() {
        assertThat(LanguageMapper.getLabelRes("invalid")).isEqualTo(R.string.comic_header_unknown)
        assertThat(LanguageMapper.getLabelRes("")).isEqualTo(R.string.comic_header_unknown)
    }

    @Test
    fun `deve retornar todos os codigos de idiomas suportados`() {
        val codes = LanguageMapper.getAllCodes()
        assertThat(codes).contains("pt-br")
        assertThat(codes).contains("en")
        assertThat(codes).contains("ja")
    }
}
