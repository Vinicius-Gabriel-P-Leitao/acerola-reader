package br.acerola.manga.domain.model.archive

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "manga_folder"
)
data class MangaFolder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "cover")
    val cover: Boolean,

    @ColumnInfo(name = "banner")
    val banner: Boolean,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long
)
