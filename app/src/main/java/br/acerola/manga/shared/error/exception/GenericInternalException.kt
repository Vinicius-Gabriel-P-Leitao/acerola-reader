package br.acerola.manga.shared.error.exception

import android.util.Log
import br.acerola.manga.R

// TODO: Criar uma string
class GenericInternalException(
    cause: Throwable
) : ApplicationException(
    title = R.string.title_generic_internal_error,
    description = R.string.description_generic_internal_error,
) {
    init {
        Log.e("GenericInternalError", cause.message.toString())
        initCause(cause)
    }
}