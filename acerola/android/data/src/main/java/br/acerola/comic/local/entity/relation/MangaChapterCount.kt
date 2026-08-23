package br.acerola.comic.local.entity.relation

import androidx.room.ColumnInfo

data class MangaChapterCount(
    @ColumnInfo(name = "comic_directory_fk")
    val comicDirectoryFk: Long,
    val count: Int,
)
