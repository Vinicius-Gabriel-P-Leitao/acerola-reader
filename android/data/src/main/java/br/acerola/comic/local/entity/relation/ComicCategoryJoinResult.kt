package br.acerola.comic.local.entity.relation

import androidx.room.ColumnInfo

data class ComicCategoryJoinResult(
    @ColumnInfo(name = "comic_directory_fk")
    val mangaDirectoryId: Long,
    @ColumnInfo(name = "id")
    val categoryId: Long,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "color")
    val color: Int,
)
