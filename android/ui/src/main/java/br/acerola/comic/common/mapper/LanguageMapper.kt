package br.acerola.comic.common.mapper

import br.acerola.comic.pattern.LanguagePattern
import br.acerola.comic.ui.R

object LanguageMapper {
    fun getLabelRes(languageCode: String): Int =
        when (LanguagePattern.from(languageCode)) {
            LanguagePattern.PT_BR -> R.string.lang_pt_br
            LanguagePattern.EN -> R.string.lang_en
            LanguagePattern.ES_LA -> R.string.lang_es_la
            LanguagePattern.ES -> R.string.lang_es
            LanguagePattern.FR -> R.string.lang_fr
            LanguagePattern.IT -> R.string.lang_it
            LanguagePattern.DE -> R.string.lang_de
            LanguagePattern.RU -> R.string.lang_ru
            LanguagePattern.JA -> R.string.lang_ja
            LanguagePattern.KO -> R.string.lang_ko
            LanguagePattern.ZH -> R.string.lang_zh
            LanguagePattern.ID -> R.string.lang_id
            null -> R.string.comic_header_unknown
        }

    fun getAllCodes(): List<String> = LanguagePattern.entries.map { it.code }
}
