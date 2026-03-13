package br.acerola.manga.util

fun String.normalizeTitle(): String {
    return this.filter { it.isLetterOrDigit() }.lowercase().trim()
}
