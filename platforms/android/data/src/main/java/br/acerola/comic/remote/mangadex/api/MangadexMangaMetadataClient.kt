package br.acerola.comic.remote.mangadex.api

import br.acerola.comic.remote.mangadex.dto.MangaDexEntityResponseDto
import br.acerola.comic.remote.mangadex.dto.MangadexResponseDto
import br.acerola.comic.remote.mangadex.dto.comic.MangaMangadexDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MangadexMangaMetadataClient {
    @GET(value = "manga")
    suspend fun searchMangaByName(
        @Query(value = "title") title: String,
        @Query(value = "limit") limit: Int = 10,
        @Query(value = "offset") offset: Int = 0,
        @Query(value = "includes[]") includes: List<String> = listOf("author", "artist", "cover_art"),
        @Query(value = "availableTranslatedLanguage[]") languages: List<String>,
    ): MangadexResponseDto<MangaMangadexDto>

    @GET(value = "manga/{id}")
    suspend fun getMangaById(
        @Path(value = "id") comicId: String,
        @Query(value = "includes[]") includes: List<String> = listOf("author", "artist", "cover_art"),
        @Query(value = "availableTranslatedLanguage[]") languages: List<String>? = null,
    ): MangaDexEntityResponseDto<MangaMangadexDto>
}
