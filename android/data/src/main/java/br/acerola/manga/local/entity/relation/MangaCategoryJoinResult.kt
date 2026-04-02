package br.acerola.manga.local.entity.relation

import androidx.room.ColumnInfo

data class MangaCategoryJoinResult(
    @ColumnInfo(name = "manga_directory_fk")
    val mangaDirectoryId: Long,

    @ColumnInfo(name = "id")
    val categoryId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "color")
    val color: Int
)
