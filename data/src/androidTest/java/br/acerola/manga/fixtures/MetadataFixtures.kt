package br.acerola.manga.fixtures

import br.acerola.manga.local.database.entity.metadata.ChapterDownloadSource
import br.acerola.manga.local.database.entity.metadata.ChapterRemoteInfo
import br.acerola.manga.local.database.entity.metadata.MangaRemoteInfo
import br.acerola.manga.local.database.entity.metadata.relationship.Author
import br.acerola.manga.local.database.entity.metadata.relationship.Cover
import br.acerola.manga.local.database.entity.metadata.relationship.Genre
import br.acerola.manga.local.database.entity.metadata.relationship.TypeAuthor

object MetadataFixtures {

    fun createMangaRemoteInfo(
        id: Long = 0,
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
        id: Long = 0,
        title: String? = "Episode 1",
        chapter: String = "1",
        pageCount: Int = 20,
        scanlation: String? = "ScanGroup",
        mangaRemoteInfoFk: Long = 0
    ) = ChapterRemoteInfo(
        id = id,
        title = title,
        chapter = chapter,
        pageCount = pageCount,
        scanlation = scanlation,
        mangaRemoteInfoFk = mangaRemoteInfoFk
    )

    fun createChapterDownloadSource(
        id: Long = 0,
        pageNumber: Int = 0,
        imageUrl: String = "http://img.com/1.jpg",
        downloaded: Boolean = false,
        chapterFk: Long = 0
    ) = ChapterDownloadSource(
        id = id,
        pageNumber = pageNumber,
        imageUrl = imageUrl,
        downloaded = downloaded,
        chapterFk = chapterFk
    )

    fun createAuthor(
        id: Long = 0,
        name: String = "Kishimoto",
        type: TypeAuthor = TypeAuthor.AUTHOR,
        mirrorId: String = "auth-1",
        mangaId: Long = 0
    ) = Author(
        id = id, 
        name = name, 
        type = type, 
        mirrorId = mirrorId, 
        mangaRemoteInfoFk = mangaId
    )

    fun createGenre(
        id: Long = 0,
        genre: String = "Shonen",
        mirrorId: String = "gen-1",
        mangaId: Long = 0
    ) = Genre(
        id = id, 
        genre = genre, 
        mirrorId = mirrorId, 
        mangaRemoteInfoFk = mangaId
    )

    fun createCover(
        id: Long = 0,
        url: String = "http://cover.jpg",
        fileName: String = "cover.jpg",
        mirrorId: String = "cov-1",
        mangaId: Long = 0
    ) = Cover(
        id = id, 
        url = url, 
        fileName = fileName, 
        mirrorId = mirrorId, 
        mangaRemoteInfoFk = mangaId
    )
}