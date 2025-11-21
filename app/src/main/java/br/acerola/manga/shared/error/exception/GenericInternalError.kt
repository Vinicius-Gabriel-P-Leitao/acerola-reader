package br.acerola.manga.shared.error.exception


// TODO: Criar uma string
class GenericInternalError(
    cause: Throwable
) : ApplicationException(
    title = "Erro interno desconhecido.",
    description = "Ocorreu um erro interno inesperado.",
) {
    init {
        initCause(cause)
    }
}