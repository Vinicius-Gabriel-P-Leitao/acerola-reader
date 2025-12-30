package br.acerola.manga.shared.util

import br.acerola.manga.shared.error.exception.ApplicationException
import br.acerola.manga.shared.error.exception.GenericInternalException
import br.acerola.manga.shared.error.handler.GlobalErrorHandler

suspend inline fun <T> runErrorHandled(block: () -> T): T? {
    return try {
        block()
    } catch (exception: ApplicationException) {
        GlobalErrorHandler.emit(exception)
        null
    } catch (throwable: Throwable) {
        val internal = GenericInternalException(cause = throwable)
        GlobalErrorHandler.emit(exception = internal)
        null
    }
}