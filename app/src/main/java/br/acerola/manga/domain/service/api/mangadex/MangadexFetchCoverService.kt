package br.acerola.manga.domain.service.api.mangadex

import br.acerola.manga.R
import br.acerola.manga.domain.data.dao.api.mangadex.cover.MangaDexDownloadDao
import br.acerola.manga.domain.service.api.ApiPort
import br.acerola.manga.shared.error.exception.MangadexRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangadexFetchCoverService @Inject constructor(
    private val api: MangaDexDownloadDao
) : ApiPort.ArchiveOperations<String> {
    override suspend fun searchCover(url: String, vararg extra: String?): ByteArray {
        return withContext(context = Dispatchers.IO) {
            try {
                val responseBody = api.downloadFile(fileUrl = url)
                val bytes = responseBody.bytes()
                bytes
            } catch (exception: Exception) {
                throw MangadexRequestException(
                    title = R.string.title_download_error,
                    description = R.string.description_error_download_failed
                )
            }
        }
    }
}
