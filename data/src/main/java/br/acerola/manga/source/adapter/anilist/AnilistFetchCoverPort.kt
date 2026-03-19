package br.acerola.manga.source.adapter.anilist

import arrow.core.Either
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.config.network.safeApiCall
import br.acerola.manga.remote.mangadex.api.MangadexDownloadApi
import br.acerola.manga.source.port.BinaryOperationsPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnilistFetchCoverPort @Inject constructor(
    private val api: MangadexDownloadApi
) : BinaryOperationsPort<String> {
    override suspend fun searchCover(url: String, vararg extra: String?): Either<NetworkError, ByteArray> =
        safeApiCall {
            withContext(Dispatchers.IO) {
                api.downloadFile(fileUrl = url).bytes()
            }
        }
}
