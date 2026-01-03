package br.acerola.manga.repository.adapter.remote.mangadex.manga

import br.acerola.manga.data.R
import br.acerola.manga.error.exception.MangadexRequestException
import br.acerola.manga.remote.mangadex.api.MangadexDownloadApi
import br.acerola.manga.repository.port.ApiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexFetchCoverService @Inject constructor(
    private val api: MangadexDownloadApi
) : ApiRepository.ArchiveOperations<String> {
    override suspend fun searchCover(url: String, vararg extra: String?): ByteArray {
        return withContext(context = Dispatchers.IO) {
            try {
                val responseBody = api.downloadFile(fileUrl = url)
                val bytes = responseBody.bytes()

                bytes
            } catch (_: Exception) {
                throw MangadexRequestException(
                    title = R.string.title_download_error,
                    description = R.string.description_error_download_failed
                )
            }
        }
    }
}
