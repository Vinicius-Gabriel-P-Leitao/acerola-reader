package br.acerola.manga.fixtures

import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.CoverDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.local.database.entity.metadata.ChapterDownloadSource
import br.acerola.manga.local.database.entity.metadata.ChapterRemoteInfo
import br.acerola.manga.local.database.entity.metadata.MangaRemoteInfo
import br.acerola.manga.local.database.entity.metadata.relationship.Author
import br.acerola.manga.local.database.entity.metadata.relationship.Cover
import br.acerola.manga.local.database.entity.metadata.relationship.Genre
import br.acerola.manga.local.database.entity.metadata.relationship.TypeAuthor
import br.acerola.manga.local.database.entity.relation.RemoteInfoRelations

object MetadataFixtures {

    fun createMangaRemoteInfo(
        id: Long = 10,
        mirrorId: String = "manga-123",
        title: String = "Naruto",
        description: String = "Ninja story",
        romanji: String = "Naruto",
        status: String = "ongoing",
        publication: Int = 1999
    ) = MangaRemoteInfo(
        id = id,
        mirrorId = mirrorId,
        title = title,
        description = description,
        romanji = romanji,
        status = status,
        publication = publication
    )

    fun createChapterRemoteInfo(
        id: Long = 100,
        title: String? = "Episode 1",
        chapter: String = "1",
        pageCount: Int = 20,
        scanlation: String? = "ScanGroup",
        mangaRemoteInfoFk: Long = 10
    ) = ChapterRemoteInfo(
        id = id,
        title = title,
        chapter = chapter,
        pageCount = pageCount,
        scanlation = scanlation,
        mangaRemoteInfoFk = mangaRemoteInfoFk
    )

    fun createChapterDownloadSource(
        id: Long = 1000,
        pageNumber: Int = 0,
        imageUrl: String = "http://img.com/1.jpg",
        downloaded: Boolean = false,
        chapterFk: Long = 100
    ) = ChapterDownloadSource(
        id = id,
        pageNumber = pageNumber,
        imageUrl = imageUrl,
        downloaded = downloaded,
        chapterFk = chapterFk
    )

    fun createChapterRemoteInfoDto(
        id: String = "ch-1",
        chapter: String = "1",
        mangadexVersion: Int = 1,
        title: String? = null
    ) = ChapterRemoteInfoDto(
        id = id,
        chapter = chapter,
        mangadexVersion = mangadexVersion,
        title = title
    )

    fun createMangaRemoteInfoDto(
        mirrorId: String = "manga-123",
        title: String = "Naruto",
        description: String = "Desc",
        status: String = "ongoing",
        year: Int? = null,
        authors: AuthorDto? = null,
        genre: List<GenreDto> = emptyList(),
        cover: CoverDto? = null
    ) = MangaRemoteInfoDto(
        mirrorId = mirrorId,
        title = title,
        description = description,
        status = status,
        year = year,
        authors = authors,
        genre = genre,
        cover = cover
    )
    
    fun createRemoteInfoRelations(
        remoteInfo: MangaRemoteInfo = createMangaRemoteInfo(),
        authors: List<Author> = emptyList(),
        covers: List<Cover> = emptyList(),
        genres: List<Genre> = emptyList()
    ) = RemoteInfoRelations(
        remoteInfo = remoteInfo,
        author = authors,
        cover = covers,
        genre = genres
    )

    fun createAuthor(
        id: Long = 1,
        name: String = "Kishimoto",
        type: TypeAuthor = TypeAuthor.AUTHOR,
        mirrorId: String = "auth-1",
        mangaId: Long = 10
    ) = Author(id = id, name = name, type = type, mirrorId = mirrorId, mangaRemoteInfoFk = mangaId)

    fun createGenre(
        id: Long = 1,
        genre: String = "Shonen",
        mirrorId: String = "gen-1",
        mangaId: Long = 10
    ) = Genre(id = id, genre = genre, mirrorId = mirrorId, mangaRemoteInfoFk = mangaId)

    fun createCover(
        id: Long = 1,
        url: String = "http://cover.jpg",
        fileName: String = "cover.jpg",
        mirrorId: String = "cov-1",
        mangaId: Long = 10
    ) = Cover(id = id, url = url, fileName = fileName, mirrorId = mirrorId, mangaRemoteInfoFk = mangaId)
}
