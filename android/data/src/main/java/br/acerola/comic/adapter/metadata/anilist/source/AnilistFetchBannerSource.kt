package br.acerola.comic.adapter.metadata.anilist.source

import arrow.core.Either
import br.acerola.comic.adapter.contract.provider.ImageProvider
import br.acerola.comic.config.network.safeApiCall
import br.acerola.comic.error.message.NetworkError
import br.acerola.comic.remote.mangadex.api.MangadexMangaDownloadClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnilistFetchBannerSource @Inject constructor(
    private val api: MangadexMangaDownloadClient
) : ImageProvider<String> {
    override suspend fun searchMedia(url: String, vararg extra: String?): Either<NetworkError, ByteArray> =
        safeApiCall {
            withContext(Dispatchers.IO) {
                api.downloadFile(fileUrl = url).bytes()
            }
        }
}
