package br.acerola.comic.error.exception

class IntegrityException(
    val source: String,
    val key: String,
) : RuntimeException("Violation of integrity constraint in $source for key $key.")
