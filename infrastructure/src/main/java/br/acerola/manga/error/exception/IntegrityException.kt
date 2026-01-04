package br.acerola.manga.error.exception

class IntegrityException(
    val source: String,
    val key: String
) : ApplicationException()