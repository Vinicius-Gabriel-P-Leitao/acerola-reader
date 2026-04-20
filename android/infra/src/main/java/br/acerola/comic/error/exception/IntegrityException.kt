package br.acerola.comic.error.exception

class IntegrityException(
    val source: String,
    val key: String,
) : RuntimeException("Violação de restrição de integridade em $source para chave $key")
