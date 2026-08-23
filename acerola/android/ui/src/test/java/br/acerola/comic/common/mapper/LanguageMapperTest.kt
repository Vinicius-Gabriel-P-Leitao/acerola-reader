package br.acerola.comic.common.mapper

import br.acerola.comic.ui.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LanguageMapperTest {
    @Test
    fun `should return correct string resource for each language code`() {
        assertThat(LanguageMapper.getLabelRes("pt-br")).isEqualTo(R.string.lang_pt_br)
        assertThat(LanguageMapper.getLabelRes("en")).isEqualTo(R.string.lang_en)
        assertThat(LanguageMapper.getLabelRes("ja")).isEqualTo(R.string.lang_ja)
    }

    @Test
    fun `should return unknown when language code is invalid or null`() {
        assertThat(LanguageMapper.getLabelRes("invalid")).isEqualTo(R.string.comic_header_unknown)
        assertThat(LanguageMapper.getLabelRes("")).isEqualTo(R.string.comic_header_unknown)
    }

    @Test
    fun `should return all supported language codes`() {
        val codes = LanguageMapper.getAllCodes()
        assertThat(codes).contains("pt-br")
        assertThat(codes).contains("en")
        assertThat(codes).contains("ja")
    }
}
