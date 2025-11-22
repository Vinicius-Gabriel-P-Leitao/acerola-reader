package br.acerola.manga.shared.error.exception

import androidx.annotation.StringRes
import br.acerola.manga.R

// TODO: Criar uma string
class GenericInternalError(
    cause: Throwable
) : ApplicationException(
    title = R.string.title_generic_internal_error,
    description = R.string.description_generic_internal_error,
) {
    init {
        initCause(cause)
    }
}