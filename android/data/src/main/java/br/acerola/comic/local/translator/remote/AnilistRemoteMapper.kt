package br.acerola.comic.local.translator.remote

import br.acerola.comic.dto.metadata.comic.AuthorDto
import br.acerola.comic.dto.metadata.comic.GenreDto
import br.acerola.comic.dto.metadata.comic.ComicMetadataDto
import br.acerola.comic.dto.metadata.comic.source.ComicSourcesDto
import br.acerola.comic.dto.metadata.comic.source.AnilistSourceDto
import br.acerola.comic.remote.anilist.MediaDetailsQuery
import br.acerola.comic.remote.anilist.MediaSearchQuery

fun MediaSearchQuery.Medium.toViewDto(): ComicMetadataDto {
    val title = this.title?.userPreferred ?: this.title?.romaji ?: ""

    val author = this.staff?.edges.orEmpty()
        .firstOrNull { edge ->
            val role = edge?.role.orEmpty()
            role.contains("Story", ignoreCase = true) || role.contains("Art", ignoreCase = true)
        }
        ?.let { edge ->
            val name = edge.node?.name?.full ?: return@let null
            val role = edge.role ?: "author"
            AuthorDto(id = "anilist-author", name = name, type = role.lowercase())
        }

    val genres = this.genres.orEmpty().mapNotNull { genreName ->
        if (!genreName.isNullOrBlank()) GenreDto(id = "anilist-$genreName", name = genreName) else null
    }

    return ComicMetadataDto(
        title = title,
        romanji = this.title?.romaji,
        description = this.description?.cleanHtml() ?: "",
        status = this.status?.name ?: "UNKNOWN",
        year = this.startDate?.year,
        authors = author,
        genre = genres,
        sources = ComicSourcesDto(
            anilist = AnilistSourceDto(
                anilistId = this.id,
                averageScore = this.averageScore,
                popularity = this.popularity,
                trending = this.trending,
                coverImage = this.coverImage?.large,
                bannerImage = this.bannerImage
            )
        )
    )
}

fun MediaDetailsQuery.Media.toViewDto(): ComicMetadataDto {
    val title = this.title?.userPreferred ?: this.title?.romaji ?: ""

    val author = this.staff?.edges.orEmpty()
        .firstOrNull { edge ->
            val role = edge?.role.orEmpty()
            role.contains("Story", ignoreCase = true) || role.contains("Art", ignoreCase = true)
        }
        ?.let { edge ->
            val name = edge.node?.name?.full ?: return@let null
            val role = edge.role ?: "author"
            AuthorDto(id = "anilist-author", name = name, type = role.lowercase())
        }

    val genres = this.genres.orEmpty().mapNotNull { genreName ->
        if (!genreName.isNullOrBlank()) GenreDto(id = "anilist-$genreName", name = genreName) else null
    }

    return ComicMetadataDto(
        title = title,
        romanji = this.title?.romaji,
        description = this.description?.cleanHtml() ?: "",
        status = this.status?.name ?: "UNKNOWN",
        year = this.startDate?.year,
        authors = author,
        genre = genres,
        sources = ComicSourcesDto(
            anilist = AnilistSourceDto(
                anilistId = this.id,
                averageScore = this.averageScore,
                popularity = this.popularity,
                trending = this.trending,
                coverImage = this.coverImage?.large,
                bannerImage = this.bannerImage
            )
        )
    )
}

private fun String.cleanHtml(): String {
    return this.replace(Regex("<br\\s*/?>"), "\n")
        .replace(Regex("<[^>]*>"), "")
        .replace(Regex("&quot;"), "\"")
        .replace(Regex("&amp;"), "&")
        .replace(Regex("&rsquo;"), "'")
        .replace(Regex("&nbsp;"), " ")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
