package br.acerola.manga.domain.model.archive

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "manga_folder",
    indices = [Index(value = ["name"], unique = true)]
)
data class MangaFolder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "cover")
    val cover: String?,

    @ColumnInfo(name = "banner")
    val banner: String?,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long,

    @ColumnInfo(name = "chapter_template")
    val chapterTemplate: String?,
)
