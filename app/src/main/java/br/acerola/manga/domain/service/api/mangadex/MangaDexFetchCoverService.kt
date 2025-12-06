package br.acerola.manga.domain.service.api.mangadex

import br.acerola.manga.BuildConfig
import br.acerola.manga.R
import br.acerola.manga.domain.database.dao.api.mangadex.cover.MangaDexDownloadDao
import br.acerola.manga.domain.service.api.ApiPort
import br.acerola.manga.shared.error.exception.MangaDexRequestError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class MangaDexFetchCoverService : ApiPort.ArchiveOperations<String> {
    private val api: MangaDexDownloadDao

    init {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(timeout = 30, unit = TimeUnit.SECONDS)
            .readTimeout(timeout = 30, unit = TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl(BuildConfig.MANGADEX_BASE_URL)
            .client(okHttpClient)
            .build()
            .create(MangaDexDownloadDao::class.java)
    }

    override suspend fun searchCover(url: String, vararg extra: String?): ByteArray {
        return withContext(context = Dispatchers.IO) {
            try {
                val responseBody = api.downloadFile(fileUrl = url)

                val bytes = responseBody.bytes()
                bytes
            } catch (exception: Exception) {
                throw MangaDexRequestError(
                    title = R.string.title_download_error,
                    description = R.string.description_error_download_failed
                )
            }
        }
    }
}