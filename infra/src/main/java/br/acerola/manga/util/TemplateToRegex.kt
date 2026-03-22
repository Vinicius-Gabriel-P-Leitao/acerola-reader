package br.acerola.manga.util

import br.acerola.manga.pattern.ArchiveFormatPattern

/**
 * Converte um template de usuário em uma Regex robusta.
 * O objetivo é ser tolerante a espaços e tratar o '*' como curinga universal.
 */
fun templateToRegex(template: String): Regex {
    val extensions = ArchiveFormatPattern.entries.joinToString("|") { it.name.lowercase() }
    
    var pattern = template
        .trim()
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace("{value}", "(\\d+)")
        .replace("{sub}", "(?:[.,](\\d+))?")
        // A macro de extensão agora captura o ponto opcionalmente para ser flexível
        .replace("{extension}", "\\.?($extensions)")
        .replace(" ", "\\s*")
        // Trata o ponto literal: se for um ponto seguido de *, vira curinga. Se for ponto sozinho, vira ponto literal.
        .replace(".*", ".*?")
        .replace("*", ".*?")
        // Garante que pontos literais restantes sejam escapados
        .replace(Regex("(?<!\\\\)\\."), "\\.")

    return Regex(pattern = "^$pattern$", option = RegexOption.IGNORE_CASE)
}
