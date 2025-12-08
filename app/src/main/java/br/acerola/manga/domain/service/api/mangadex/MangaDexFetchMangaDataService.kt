package br.acerola.manga.domain.service.api.mangadex

import br.acerola.manga.BuildConfig
import br.acerola.manga.R
import br.acerola.manga.domain.builder.MetadataBuilder
import br.acerola.manga.domain.database.dao.api.mangadex.manga.MangaDataMangaDexDao
import br.acerola.manga.domain.middleware.MangaDexInterceptor
import br.acerola.manga.domain.service.api.ApiPort
import br.acerola.manga.shared.dto.mangadex.MangaDexResponse
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import br.acerola.manga.shared.error.exception.MangaDexRequestError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MangaDexFetchMangaDataService(
    private val api: MangaDataMangaDexDao
) : ApiPort.MetadataOperations<MangaMetadataDto, String> {

    constructor(baseUrl: String = BuildConfig.MANGADEX_BASE_URL) : this(
        api = createDefaultApi(baseUrl)
    )

    companion object {
        private fun createDefaultApi(baseUrl: String): MangaDataMangaDexDao {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(interceptor = MangaDexInterceptor())
                .connectTimeout(timeout = 30, unit = TimeUnit.SECONDS)
                .writeTimeout(timeout = 30, unit = TimeUnit.SECONDS)
                .readTimeout(timeout = 30, unit = TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MangaDataMangaDexDao::class.java)
        }
    }

    // TODO: Criar string
    // TODO: Criar uma lógica de catch mais robusta
    override suspend fun searchManga(
        title: String, limit: Int, offset: Int, vararg extra: String?
    ): List<MangaMetadataDto> {
        return withContext(context = Dispatchers.IO) {
            try {
                val response: MangaDexResponse = api.searchMangaByName(title, limit, offset)
                MetadataBuilder.fromMangaDataList(dataList = response.data)
            } catch (httpException: HttpException) {
                val code = httpException.code()

                throw MangaDexRequestError(
                    title = R.string.title_http_error,
                    description = if (code == 429) R.string.description_http_error_rate_limit else R.string.description_http_error_generic
                )
            } catch (_: Exception) {
                throw MangaDexRequestError(
                    title = R.string.title_metadata_request_error,
                    description = R.string.description_metadata_request_error
                )
            }
        }
    }
}
