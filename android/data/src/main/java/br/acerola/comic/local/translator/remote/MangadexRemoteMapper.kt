package br.acerola.comic.local.translator.remote

import android.content.Context
import br.acerola.comic.data.R
import br.acerola.comic.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.comic.dto.metadata.comic.AuthorDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.dto.metadata.comic.CoverDto
import br.acerola.comic.dto.metadata.comic.GenreDto
import br.acerola.comic.dto.metadata.comic.source.ComicSourcesDto
import br.acerola.comic.dto.metadata.comic.source.MangadexSourceDto
import br.acerola.comic.remote.mangadex.dto.chapter.ChapterMangadexDto
import br.acerola.comic.remote.mangadex.dto.chapter.ChapterSourceMangadexDto
import br.acerola.comic.remote.mangadex.dto.manga.MangaMangadexDto

fun MangaMangadexDto.toViewDto(
    context: Context,
    preferredLanguage: String = "pt-br",
): ComicMetadataDto {
    val attributes = this.attributes

    val authors =
        if (this.authorName != null && this.authorId != null) {
            AuthorDto(
                id = this.authorId!!,
                name = this.authorName!!,
                type = this.authorType!!,
            )
        } else {
            null
        }

    val coverDto =
        if (this.coverFileName != null && this.coverId != null) {
            CoverDto(
                id = this.coverId!!,
                url = this.getCoverUrl() ?: "",
                fileName = this.coverFileName!!,
            )
        } else {
            null
        }

    val genresList: List<GenreDto> =
        attributes.tags.mapNotNull {
            val name = it.attributes.getName(preferredLanguage)

            if (!name.isNullOrBlank()) {
                GenreDto(
                    id = it.id,
                    name = name,
                )
            } else {
                null
            }
        }

    val romanji: String? =
        attributes.altTitlesList
            .flatMap { it.entries }
            .find { it.key == "ja-ro" }
            ?.value
            ?: attributes.titleMap["ja-ro"]

    val mangadexSourceDto =
        MangadexSourceDto(
            mangadexId = this.id,
            anilistId = attributes.links?.al,
            amazonUrl = attributes.links?.amz,
            ebookjapanUrl = attributes.links?.ebj,
            rawUrl = attributes.links?.raw,
            engtlUrl = attributes.links?.engtl,
        )

    return ComicMetadataDto(
        title = attributes.getTitle(preferredLanguage) ?: context.getString(R.string.description_manga_untitled),
        description = attributes.getDescription(preferredLanguage) ?: "",
        romanji = romanji,
        year = attributes.year,
        status = attributes.status,
        cover = coverDto,
        genre = genresList,
        authors = authors,
        sources =
            ComicSourcesDto(
                mangadex = mangadexSourceDto,
            ),
    )
}

fun ChapterMangadexDto.toViewDto(source: ChapterSourceMangadexDto? = null): ChapterMetadataDto {
    val attributes = this.attributes
    val scanlatorName = this.scanlationGroups.firstNotNullOfOrNull { it.attributes?.name }

    val pagesUrls =
        if (source != null) {
            val dataSaver = source.chapter
            val baseUrl = source.baseUrl
            val hash = dataSaver.hash

            dataSaver.data.map { "$baseUrl/data/$hash/$it" }
        } else {
            emptyList()
        }

    return ChapterMetadataDto(
        id = this.id,
        volume = attributes.volume,
        chapter = attributes.chapter,
        title = attributes.title,
        scanlator = scanlatorName,
        pages = attributes.pages,
        mangadexVersion = this.version,
        pageUrls = pagesUrls,
    )
}
