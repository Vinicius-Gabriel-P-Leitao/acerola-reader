package br.acerola.comic.type

enum class Language(
    val code: String,
) {
    PT_BR("pt-br"),
    EN("en"),
    ES_LA("es-la"),
    ES("es"),
    FR("fr"),
    IT("it"),
    DE("de"),
    RU("ru"),
    JA("ja"),
    KO("ko"),
    ZH("zh"),
    ID("id"),
    ;

    companion object {
        fun from(code: String?): Language? = entries.find { it.code.equals(code, ignoreCase = true) }
    }
}
