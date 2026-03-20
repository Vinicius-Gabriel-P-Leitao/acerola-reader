package br.acerola.manga.local.translator

import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.remote.anilist.MediaDetailsQuery
import br.acerola.manga.remote.anilist.MediaSearchQuery

fun MediaSearchQuery.Medium.toDto(): MangaRemoteInfoDto {
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

    return MangaRemoteInfoDto(
        anilistId = this.id.toString(),
        title = title,
        romanji = this.title?.romaji,
        description = this.description?.cleanHtml() ?: "",
        status = this.status?.name ?: "UNKNOWN",
        year = this.startDate?.year,
        authors = author,
        genre = genres,
        anilistScore = this.averageScore,
        anilistPopularity = this.popularity,
        anilistTrending = this.trending,
        anilistCoverImage = this.coverImage?.large,
        anilistBannerImage = this.bannerImage
    )
}

fun MediaDetailsQuery.Media.toDto(): MangaRemoteInfoDto {
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

    return MangaRemoteInfoDto(
        anilistId = this.id.toString(),
        title = title,
        romanji = this.title?.romaji,
        description = this.description?.cleanHtml() ?: "",
        status = this.status?.name ?: "UNKNOWN",
        year = this.startDate?.year,
        authors = author,
        genre = genres,
        anilistScore = this.averageScore,
        anilistPopularity = this.popularity,
        anilistTrending = this.trending,
        anilistCoverImage = this.coverImage?.large,
        anilistBannerImage = this.bannerImage
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
