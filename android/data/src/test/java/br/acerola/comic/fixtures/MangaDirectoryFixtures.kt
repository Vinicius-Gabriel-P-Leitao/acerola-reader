package br.acerola.comic.fixtures

import android.net.Uri
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.local.entity.archive.ComicDirectory

object MangaDirectoryFixtures {

    fun createMangaDirectory(
        id: Long = 1L,
        name: String = "Manga Test",
        path: String = "content://path/test",
        cover: String? = null,
        banner: String? = null,
        lastModified: Long = 1000L,
        chapterTemplateFk: Long? = null,
    ) = ComicDirectory(
        id = id,
        name = name,
        path = path,
        cover = cover,
        banner = banner,
        lastModified = lastModified,
        chapterTemplateFk = chapterTemplateFk,
    )

    fun createMangaDirectoryDto(
        id: Long = 1L,
        name: String = "Manga Test",
        path: String = "content://path/test",
        coverUri: Uri? = null,
        bannerUri: Uri? = null,
        lastModified: Long = 1000L,
        chapterTemplateFk: Long? = null,
    ) = ComicDirectoryDto(
        id = id,
        name = name,
        path = path,
        coverUri = coverUri,
        bannerUri = bannerUri,
        lastModified = lastModified,
        chapterTemplateFk = chapterTemplateFk,
    )
}
