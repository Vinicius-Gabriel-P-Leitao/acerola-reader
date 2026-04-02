package br.acerola.manga.pattern

object MangadexPattern {

    /**
     * Extrai o UUID de uma URL completa do MangaDex.
     *
     * Exemplo de match: `https://mangadex.org/title/a96676e5-8ae2-425e-b549-7f15dd34a6d8/titulo-do-manga`
     * Grupo 1 capturado: `a96676e5-8ae2-425e-b549-7f15dd34a6d8`
     */
    val titleUrl: Regex = Regex("mangadex\\.org/title/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})")

    /**
     * Valida se uma string é um UUID v4 isolado (sem nada em volta).
     *
     * Exemplo de match: `a96676e5-8ae2-425e-b549-7f15dd34a6d8`
     * Case-insensitive — aceita letras maiúsculas e minúsculas.
     */
    val uuid: Regex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)
}
