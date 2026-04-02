package br.acerola.manga.local.translator.infra

import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaMetadataDto
import br.acerola.manga.dto.metadata.manga.source.ComicInfoSourceDto
import br.acerola.manga.dto.metadata.manga.source.MangaSourcesDto

/**
 * Representa os dados brutos extraídos do arquivo ComicInfo.xml
 */
data class ParsedComicInfo(
    val title: String,
    val series: String = "",
    val writer: String = "",
    val genres: String = "",
    val summary: String = "",
    val year: Int? = null,
    val number: String = "",
    val volume: String = "",
    val pageCount: Int = 0
)

fun ParsedComicInfo.toMangaDto(): MangaMetadataDto {
    val finalTitle = series.ifBlank { title }
    return MangaMetadataDto(
        title = finalTitle,
        description = summary,
        year = year,
        status = "Unknown",
        authors = if (writer.isNotBlank()) AuthorDto(id = "local-author", name = writer, type = "author") else null,
        genre = genres.split(",", ";").mapNotNull {
            val g = it.trim()
            if (g.isNotBlank()) GenreDto(id = "local-$g", name = g) else null
        },
        sources = MangaSourcesDto(
            comicInfo = ComicInfoSourceDto(
                localHash = "local-${finalTitle.hashCode()}"
            )
        )
    )
}

fun ParsedComicInfo.toChapterDto(): ChapterMetadataDto = ChapterMetadataDto(
    id = "local-$number",
    chapter = number,
    volume = volume,
    title = title,
    pages = pageCount,
    mangadexVersion = 0
)
