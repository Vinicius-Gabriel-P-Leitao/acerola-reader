package br.acerola.comic.local.entity.relation

import androidx.room.ColumnInfo

// TODO: Pensar em nome melhor
data class ComicCategoryJoinResult(
    @ColumnInfo(name = "comic_directory_fk")
    val comicDirectoryId: Long,
    @ColumnInfo(name = "id")
    val categoryId: Long,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "color")
    val color: Int,
)
