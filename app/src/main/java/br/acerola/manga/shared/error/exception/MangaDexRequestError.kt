package br.acerola.manga.shared.error.exception

class MangaDexRequestError(
    override val title: String = "Erro no mangadex.",
    override val description: String = "Não foi possível fazer request para o mangadex.",
) : ApplicationException(description = description)