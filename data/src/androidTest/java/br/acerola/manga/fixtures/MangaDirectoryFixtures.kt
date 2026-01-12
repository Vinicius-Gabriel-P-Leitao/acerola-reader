package br.acerola.manga.fixtures

import android.net.Uri
import br.acerola.manga.local.database.entity.archive.MangaDirectory

object MangaDirectoryFixtures {

    fun createMangaDirectory(
        id: Long = 0,
        name: String = "Manga Test",
        path: String = "content://path/test",
        cover: String? = null,
        banner: String? = null,
        lastModified: Long = 1000L,
        chapterTemplate: String? = null
    ) = MangaDirectory(
        id = id,
        name = name,
        path = path,
        cover = cover,
        banner = banner,
        lastModified = lastModified,
        chapterTemplate = chapterTemplate
    )
}
