package br.acerola.manga.domain.service.mangadex

import android.net.Uri
import br.acerola.manga.BuildConfig
import br.acerola.manga.domain.builder.MetadataBuilder
import br.acerola.manga.domain.database.dao.api.mangadex.manga.MangaDataMangaDexDao
import br.acerola.manga.shared.dto.mangadex.MangaDexResponse
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import br.acerola.manga.shared.error.MangaDexRequestError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class FetchMangaDataMangaDexService(
    baseUrl: String = BuildConfig.MANGADEX_BASE_URL
) {
    private val api: MangaDataMangaDexDao

    init {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(timeout = 30, unit = TimeUnit.SECONDS)
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
    suspend fun searchManga(uri: Uri, limit: Int = 10, offset: Int = 0): List<MangaMetadataDto> {
        return withContext(context = Dispatchers.IO) {
            try {
                val title = uri.getQueryParameter("macaco")
                    ?: throw MangaDexRequestError(
                        title = "Requisição de metadados.",
                        description = "Não foi possivel encontrar o campo na resposta do mangadex."
                    )

                val response: MangaDexResponse = api.searchMangaByName(title, limit, offset)
                MetadataBuilder.fromMangaDataList(dataList = response.data)
            } catch (_: Exception) {
                throw MangaDexRequestError(
                    title = "Requisição de metadados.",
                    description = "Erro ao fazer busca de metadados dentro do mangadex."
                )
            }
        }
    }
}