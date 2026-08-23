package br.acerola.comic.fixtures

import android.net.Uri
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.local.entity.archive.ComicDirectory

object MangaDirectoryFixtures {
    fun createMangaDirectory(
        id: Long = 1L,
        name: String = "Comic Test",
        path: String = "content://path/test",
        cover: String? = null,
        banner: String? = null,
        lastModified: Long = 1000L,
        archiveTemplateFk: Long? = null,
    ) = ComicDirectory(
        id = id,
        name = name,
        path = path,
        cover = cover,
        banner = banner,
        lastModified = lastModified,
        archiveTemplateFk = archiveTemplateFk,
    )

    fun createMangaDirectoryDto(
        id: Long = 1L,
        name: String = "Comic Test",
        path: String = "content://path/test",
        coverUri: Uri? = null,
        bannerUri: Uri? = null,
        lastModified: Long = 1000L,
        archiveTemplateFk: Long? = null,
    ) = ComicDirectoryDto(
        id = id,
        name = name,
        path = path,
        coverUri = coverUri,
        bannerUri = bannerUri,
        lastModified = lastModified,
        archiveTemplateFk = archiveTemplateFk,
    )
}
