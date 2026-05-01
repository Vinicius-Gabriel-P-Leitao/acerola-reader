package br.acerola.comic.common.mapper

import br.acerola.comic.type.Language
import br.acerola.comic.ui.R

object LanguageMapper {
    fun getLabelRes(languageCode: String): Int =
        when (Language.from(languageCode)) {
            Language.PT_BR -> R.string.lang_pt_br
            Language.EN -> R.string.lang_en
            Language.ES_LA -> R.string.lang_es_la
            Language.ES -> R.string.lang_es
            Language.FR -> R.string.lang_fr
            Language.IT -> R.string.lang_it
            Language.DE -> R.string.lang_de
            Language.RU -> R.string.lang_ru
            Language.JA -> R.string.lang_ja
            Language.KO -> R.string.lang_ko
            Language.ZH -> R.string.lang_zh
            Language.ID -> R.string.lang_id
            null -> R.string.comic_header_unknown
        }

    fun getAllCodes(): List<String> = Language.entries.map { it.code }
}
