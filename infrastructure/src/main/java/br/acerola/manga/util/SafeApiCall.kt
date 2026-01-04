package br.acerola.manga.util

import br.acerola.manga.error.exception.MangadexRequestException
import br.acerola.manga.infrastructure.R
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeApiCall(
    times: Int = 3, initialDelay: Long = 2000, call: suspend () -> T
): T {
    var currentDelay = initialDelay

    repeat(times) { attempt ->
        try {
            return call()
        } catch (httpException: HttpException) {
            if (attempt == times - 1 || httpException.code() != 429) {
                throw MangadexRequestException(
                    title = R.string.title_http_error, description = when (httpException.code()) {
                        429 -> R.string.description_http_error_rate_limit
                        404 -> R.string.description_not_found
                        else -> R.string.description_http_error_generic
                    }
                )
            }

            delay(timeMillis = currentDelay)
        } catch (_: IOException) {
            if (attempt == times - 1) {
                throw MangadexRequestException(
                    title = R.string.title_network_error,
                    description = R.string.description_network_error
                )
            }

            delay(timeMillis = currentDelay)
            currentDelay *= 2
        } catch (exception: Exception) {
            throw exception
        }
    }

    return call()
}