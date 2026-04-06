package br.acerola.comic.local.translator.infra

import br.acerola.comic.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.comic.dto.metadata.comic.AuthorDto
import br.acerola.comic.dto.metadata.comic.GenreDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.dto.metadata.comic.source.ComicInfoSourceDto
import br.acerola.comic.dto.metadata.comic.source.ComicSourcesDto

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

fun ParsedComicInfo.toMangaDto(): ComicMetadataDto {
    val finalTitle = series.ifBlank { title }
    return ComicMetadataDto(
        title = finalTitle,
        description = summary,
        year = year,
        status = "Unknown",
        authors = if (writer.isNotBlank()) AuthorDto(id = "local-author", name = writer, type = "author") else null,
        genre = genres.split(",", ";").mapNotNull {
            val g = it.trim()
            if (g.isNotBlank()) GenreDto(id = "local-$g", name = g) else null
        },
        sources = ComicSourcesDto(
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
