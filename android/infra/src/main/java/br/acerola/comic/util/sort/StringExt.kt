package br.acerola.comic.util.sort

fun String.normalizeSort(): String {
    val clean = this.replace(',', '.').trim()
    val isNumeric = clean.toDoubleOrNull() != null

    if (!isNumeric) return clean

    val parts = clean.split('.')
    val integerPart = parts[0].padStart(10, '0')
    val fractionalPart = parts.getOrNull(1)?.padEnd(10, '0') ?: "0".padEnd(10, '0')

    return "$integerPart.$fractionalPart"
}
