package br.acerola.manga.shared.error.handler

import android.util.Log
import br.acerola.manga.shared.error.exception.ApplicationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow

object GlobalErrorHandler {
    private val _errors = MutableSharedFlow<ApplicationException>(replay = 1)
    val errors = _errors

    suspend fun emit(exception: ApplicationException) {
        _errors.emit(value = exception)
        Log.e("GlobalErrorHandler", exception.message.toString())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun clear() {
        _errors.resetReplayCache()
    }
}
