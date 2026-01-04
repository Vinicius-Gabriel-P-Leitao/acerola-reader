package br.acerola.manga.util

// WARN: Verificar se esse 0 como default dá cágada
fun String.normalizeChapter(): String {
    val sanitized = this.replace(oldChar = ',', newChar =  '.').trim()

    val number = sanitized.toDoubleOrNull()
    return number?.toString() ?: sanitized.lowercase()
}