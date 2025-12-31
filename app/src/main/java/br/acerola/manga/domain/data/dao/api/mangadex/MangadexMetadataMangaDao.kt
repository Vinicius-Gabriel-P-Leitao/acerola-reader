package br.acerola.manga.domain.data.dao.api.mangadex

import br.acerola.manga.shared.dto.mangadex.MangaDexResponse
import br.acerola.manga.shared.dto.mangadex.MetadataMangaDto
import retrofit2.http.GET
import retrofit2.http.Query

interface MangadexMetadataMangaDao {
    @GET(value = "manga")
    suspend fun searchMangaByName(
        @Query(value = "title") title: String,
        @Query(value = "limit") limit: Int = 10,
        @Query(value = "offset") offset: Int = 0,
        @Query(value = "includes[]") includes: List<String> = listOf("author", "artist", "cover_art")
    ): MangaDexResponse<MetadataMangaDto>
}