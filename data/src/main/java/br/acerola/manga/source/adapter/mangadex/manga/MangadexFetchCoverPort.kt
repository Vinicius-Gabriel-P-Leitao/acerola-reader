package br.acerola.manga.source.adapter.mangadex.manga

import arrow.core.Either
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
import br.acerola.manga.config.network.safeApiCall
import br.acerola.manga.remote.mangadex.api.MangadexDownloadApi
import br.acerola.manga.source.port.BinaryOperationsPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexFetchCoverPort @Inject constructor(
    private val api: MangadexDownloadApi
) : BinaryOperationsPort<String> {
    override suspend fun searchCover(url: String, vararg extra: String?): Either<NetworkError, ByteArray> =
        safeApiCall {
        withContext(context = Dispatchers.IO) {
            AcerolaLogger.d(TAG, "Downloading cover from URL: $url", LogSource.NETWORK)  
            val responseBody = api.downloadFile(fileUrl = url)
            responseBody.bytes()
        }
    }.onLeft {
        AcerolaLogger.e(TAG, "Failed to download cover from MangaDex", LogSource.NETWORK, throwable = null)  
    }

    companion object {
        private const val TAG = "MangadexFetchCoverRepository"  
    }
}
