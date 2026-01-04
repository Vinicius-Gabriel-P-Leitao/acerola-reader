package br.acerola.manga.repository.adapter.remote.mangadex.manga

import br.acerola.manga.remote.mangadex.api.MangadexDownloadApi
import br.acerola.manga.repository.port.ApiRepository
import br.acerola.manga.util.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexFetchCoverService @Inject constructor(
    private val api: MangadexDownloadApi
) : ApiRepository.ArchiveOperations<String> {
    override suspend fun searchCover(url: String, vararg extra: String?): ByteArray = safeApiCall {
        withContext(context = Dispatchers.IO) {
            val responseBody = api.downloadFile(fileUrl = url)
            responseBody.bytes()
        }
    }
}
