package br.acerola.manga.local.mapper

import android.content.Context
import br.acerola.manga.data.R
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.CoverDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.LinksDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterMangadexDto
import br.acerola.manga.remote.mangadex.dto.chapter.ChapterSourceMangadexDto
import br.acerola.manga.remote.mangadex.dto.manga.MangaMangadexDto

fun MangaMangadexDto.toDto(context: Context): MangaRemoteInfoDto {
    val attributes = this.attributes

    val authors = if (this.authorName != null && this.authorId != null) {
        AuthorDto(
            id = this.authorId!!, name = this.authorName!!, type = this.authorType!!
        )
    } else null

    val coverDto = if (this.coverFileName != null && this.coverId != null) {
        CoverDto(
            id = this.coverId!!,
            url = this.getCoverUrl() ?: "",
            fileName = this.coverFileName!!,
        )
    } else null

    val genresList: List<GenreDto> = attributes.tags.mapNotNull {
        val name = it.attributes.name

        if (!name.isNullOrBlank()) {
            GenreDto(
                id = it.id, name = name
            )
        } else null
    }

    // NOTE: Sigla para tradução de romanji é default ja-ro
    val romanji: String? = attributes.altTitlesList.flatMap { it.entries }.find { it.key == "ja-ro" }?.value
        ?: attributes.titleMap["ja-ro"]

    val linksDto = attributes.links?.let { l ->
        LinksDto(
            anilistId = l.al,
            amazonUrl = l.amz,
            ebookjapanUrl = l.ebj,
            rawUrl = l.raw,
            engtlUrl = l.engtl
        )
    }

    return MangaRemoteInfoDto(
        mangadexId = this.id,
        anilistId = attributes.links?.al,
        title = attributes.title ?: context.getString(R.string.description_manga_untitled),
        description = attributes.description ?: "",
        romanji = romanji,
        year = attributes.year,
        status = attributes.status,
        cover = coverDto,
        genre = genresList,
        authors = authors,
        links = linksDto
    )
}

fun ChapterMangadexDto.toDto(source: ChapterSourceMangadexDto? = null): ChapterRemoteInfoDto {
    val attributes = this.attributes
    val scanlatorName = this.scanlationGroups.firstNotNullOfOrNull { it.attributes?.name }

    val pagesUrls = if (source != null) {
        val dataSaver = source.chapter
        val baseUrl = source.baseUrl
        val hash = dataSaver.hash

        dataSaver.data.map { "$baseUrl/data/$hash/$it" }
    } else {
        emptyList()
    }

    return ChapterRemoteInfoDto(
        id = this.id,
        volume = attributes.volume,
        chapter = attributes.chapter,
        title = attributes.title,
        scanlator = scanlatorName,
        pages = attributes.pages,
        mangadexVersion = this.version,
        pageUrls = pagesUrls
    )
}
