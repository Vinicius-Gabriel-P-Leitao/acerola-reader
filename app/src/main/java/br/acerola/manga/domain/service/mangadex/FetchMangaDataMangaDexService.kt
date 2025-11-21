package br.acerola.manga.domain.service.mangadex

import android.net.Uri
import br.acerola.manga.BuildConfig
import br.acerola.manga.domain.builder.MetadataBuilder
import br.acerola.manga.domain.database.dao.api.mangadex.manga.MangaDataMangaDexDao
import br.acerola.manga.shared.config.MangaDexInterceptor
import br.acerola.manga.shared.dto.mangadex.MangaDexResponse
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import br.acerola.manga.shared.error.MangaDexRequestError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class FetchMangaDataMangaDexService(
    baseUrl: String = BuildConfig.MANGADEX_BASE_URL
) {
    private val api: MangaDataMangaDexDao

    init {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor = MangaDexInterceptor())
            .connectTimeout(timeout = 30, unit = TimeUnit.SECONDS)
            .writeTimeout(timeout = 30, unit = TimeUnit.SECONDS)
            .readTimeout(timeout = 30, unit = TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MangaDataMangaDexDao::class.java)
    }


    // TODO: Criar string
    // TODO: Criar uma lógica de catch mais robusta
    suspend fun searchManga(title: String, limit: Int = 10, offset: Int = 0): List<MangaMetadataDto> {
        return withContext(context = Dispatchers.IO) {
            try {
                val response: MangaDexResponse = api.searchMangaByName(title, limit, offset)
                MetadataBuilder.fromMangaDataList(dataList = response.data)
            } catch (httpException: HttpException) {
                val code = httpException.code()

                throw MangaDexRequestError(
                    title = "Erro HTTP $code",
                    description = if (code == 429) "Muitas requisições. Tente novamente em breve." else "Erro de comunicação com o MangaDex."
                )
            } catch (_: Exception) {
                throw MangaDexRequestError(
                    title = "Requisição de metadados.",
                    description = "Erro ao fazer busca de metadados dentro do mangadex."
                )
            }
        }
    }
}