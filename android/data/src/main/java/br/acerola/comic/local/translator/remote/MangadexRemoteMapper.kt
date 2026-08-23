package br.acerola.comic.local.translator.remote

import android.content.Context
import br.acerola.comic.data.R
import br.acerola.comic.dto.metadata.comic.AuthorDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.dto.metadata.comic.CoverDto
import br.acerola.comic.dto.metadata.comic.GenreDto
import br.acerola.comic.dto.metadata.comic.source.ComicSourcesDto
import br.acerola.comic.dto.metadata.comic.source.MangadexSourceDto
import br.acerola.comic.remote.mangadex.dto.comic.MangaMangadexDto

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
        title = attributes.getTitle(preferredLanguage) ?: context.getString(R.string.description_comic_untitled),
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
