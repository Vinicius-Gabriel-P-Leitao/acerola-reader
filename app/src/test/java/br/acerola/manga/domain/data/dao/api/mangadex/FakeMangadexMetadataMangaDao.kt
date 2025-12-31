package br.acerola.manga.domain.data.dao.api.mangadex

import br.acerola.manga.data.remote.mangadex.dto.MangaDexResponse
import retrofit2.http.Query

class FakeMangadexMetadataMangaDao : MangadexMetadataMangaDao {
    var response: MangaDexResponse? = null
    var shouldThrow: Boolean = false
    var lastTitle: String? = null

    override suspend fun searchMangaByName(
        @Query(value = "title") title: String,
        @Query(value = "limit") limit: Int,
        @Query(value = "offset") offset: Int,
        @Query(value = "includes[]") includes: List<String>
    ): MangaDexResponse {
        lastTitle = title
        if (shouldThrow) throw RuntimeException("Fake error")
        return response ?: throw RuntimeException("Response not set")
    }
}
