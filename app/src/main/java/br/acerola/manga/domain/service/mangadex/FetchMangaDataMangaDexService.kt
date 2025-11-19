package br.acerola.manga.domain.service.mangadex

import br.acerola.manga.shared.config.MangaDexApiModule
import br.acerola.manga.shared.dto.mangadex.MangaDexResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TODO: Tratar melhor
class FetchMangaDataMangaDexService {
    private val api = MangaDexApiModule.mangaDataMangadexDao

    suspend fun searchManga(title: String, limit: Int = 10, offset: Int = 0): List<String> {
        return withContext(context = Dispatchers.IO) {
            try {
                val response: MangaDexResponse = api.searchMangaByName(title, limit, offset)
                response.data.mapNotNull { it.attributes.title }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}