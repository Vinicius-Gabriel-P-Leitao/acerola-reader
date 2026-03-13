package br.acerola.manga.util

fun String.normalizeChapter(): String {
    val clean = this.replace(oldChar = ',', newChar = '.').trim()

    return if (!clean.contains('.')) {
        // NOTE: Só para numeros inteiros sem fração
        clean.toIntOrNull()?.toString() ?: clean
    } else {
        val parts = clean.split('.')

        val integerPart = parts[0].toIntOrNull() ?: 0
        val fractionalPart = parts.getOrNull(index = 1)?.toIntOrNull() ?: 0

        if (fractionalPart == 0) "$integerPart" else "$integerPart.$fractionalPart"
    }
}