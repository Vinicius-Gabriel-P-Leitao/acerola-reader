package br.acerola.manga.util

// WARN: Verificar se esse 0 como default dá cágada
fun String.normalizeChapter(): String =
    replace(oldChar = ',', newChar = '.').trim().trimStart('0').ifEmpty { "0" }