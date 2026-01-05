package br.acerola.manga.network

import arrow.core.Either
import br.acerola.manga.error.message.NetworkError
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeApiCall(
    timeoutMs: Long = 1000L,
    block: suspend () -> T
): Either<NetworkError, T> {
    return Either.catch {
        // NOTE: Rate limite para que o mangadex não dê bonk
        if (timeoutMs > 0) delay(timeMillis = timeoutMs)

        block()
    }.mapLeft { exception ->
        when (exception) {
            is HttpException -> when (exception.code()) {
                429 -> NetworkError.RateLimitExceeded(retryAfter = exception.response()?.headers()?.get("Retry-After")?.toLongOrNull())

                in 500..599 -> NetworkError.ServerError(code = exception.code(), cause = exception)

                401, 403 -> NetworkError.Unauthorized(cause = exception)
                404 -> NetworkError.NotFound(cause = exception)

                else -> NetworkError.HttpError(code = exception.code(), cause = exception)
            }

            is IOException -> NetworkError.ConnectionFailed(cause = exception)
            is TimeoutCancellationException -> NetworkError.Timeout(cause = exception)
            else -> NetworkError.UnexpectedError(cause = exception)
        }
    }
}
