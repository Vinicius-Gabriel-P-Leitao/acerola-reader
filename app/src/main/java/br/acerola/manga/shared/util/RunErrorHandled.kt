package br.acerola.manga.shared.util

import br.acerola.manga.shared.error.ApplicationException
import br.acerola.manga.shared.error.GenericInternalError
import br.acerola.manga.shared.error.handler.GlobalErrorHandler

suspend inline fun <T> runErrorHandled(block: () -> T): T? {
    return try {
        block()
    } catch (exception: ApplicationException) {
        GlobalErrorHandler.emit(exception)
        null
    } catch (throwable: Throwable) {
        val internal = GenericInternalError(cause = throwable)
        GlobalErrorHandler.emit(exception = internal)
        null
    }
}