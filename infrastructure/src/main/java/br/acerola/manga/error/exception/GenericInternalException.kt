package br.acerola.manga.error.exception

import android.util.Log
import br.acerola.manga.infrastructure.R

// TODO: Criar uma string
class GenericInternalException(
    cause: Throwable
) : ApplicationException(
    title = R.string.title_generic_internal_error,
    description = R.string.description_generic_internal_error,
) {
    init {
        Log.e("GenericInternalError", cause.toString())
        Log.e("GenericInternalError", cause.localizedMessage)
        Log.e("GenericInternalError", cause.message.toString())
        Log.e("GenericInternalError", cause.stackTraceToString())
        initCause(cause)
    }
}