package br.acerola.manga.repository.adapter.remote.anilist

import arrow.core.Either
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.network.safeApiCall
import br.acerola.manga.remote.mangadex.api.MangadexDownloadApi
import br.acerola.manga.repository.port.BinaryOperationsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnilistFetchCoverRepository @Inject constructor(
    private val api: MangadexDownloadApi
) : BinaryOperationsRepository<String> {
    override suspend fun searchCover(url: String, vararg extra: String?): Either<NetworkError, ByteArray> =
        safeApiCall {
            withContext(Dispatchers.IO) {
                api.downloadFile(fileUrl = url).bytes()
            }
        }
}
