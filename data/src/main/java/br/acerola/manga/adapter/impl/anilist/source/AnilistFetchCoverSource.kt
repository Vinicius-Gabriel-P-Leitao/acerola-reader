package br.acerola.manga.adapter.impl.anilist.source

import arrow.core.Either
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.config.network.safeApiCall
import br.acerola.manga.remote.mangadex.api.MangadexDownloadApi
import br.acerola.manga.adapter.port.ImageFetchPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnilistFetchCoverSource @Inject constructor(
    private val api: MangadexDownloadApi
) : ImageFetchPort<String> {
    override suspend fun searchCover(url: String, vararg extra: String?): Either<NetworkError, ByteArray> =
        safeApiCall {
            withContext(Dispatchers.IO) {
                api.downloadFile(fileUrl = url).bytes()
            }
        }
}
