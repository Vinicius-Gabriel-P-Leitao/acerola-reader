package br.acerola.manga.error.handler

import android.util.Log
import br.acerola.manga.error.exception.ApplicationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow

@Deprecated("Use Result/Either pattern instead of global error handling")
object GlobalErrorHandler {
    private val _errors = MutableSharedFlow<ApplicationException>(replay = 1)
    val errors = _errors

    // TODO: Melhorar log
    suspend fun emit(exception: ApplicationException) {
        _errors.emit(value = exception)
        Log.e("GlobalErrorHandler", exception.message.toString())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun clear() {
        _errors.resetReplayCache()
    }
}
