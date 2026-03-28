package br.acerola.manga.remote.mangadex.api

import br.acerola.manga.remote.mangadex.dto.MangaDexEntityResponseDto
import br.acerola.manga.remote.mangadex.dto.MangaDexResponse
import br.acerola.manga.remote.mangadex.dto.manga.MangaMangadexDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MangadexMangaMetadataClient {
    @GET(value = "manga")
    suspend fun searchMangaByName(
        @Query(value = "title") title: String,
        @Query(value = "limit") limit: Int = 10,
        @Query(value = "offset") offset: Int = 0,
        @Query(value = "includes[]") includes: List<String> = listOf("author", "artist", "cover_art")
    ): MangaDexResponse<MangaMangadexDto>

    @GET(value = "manga/{id}")
    suspend fun getMangaById(
        @Path(value = "id") mangaId: String,
        @Query(value = "includes[]") includes: List<String> = listOf("author", "artist", "cover_art")
    ): MangaDexEntityResponseDto<MangaMangadexDto>
}