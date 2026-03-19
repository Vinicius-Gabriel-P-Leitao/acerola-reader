package br.acerola.manga.local.entity.archive

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapter_archive",
    indices = [
        Index(value = ["manga_directory_fk", "chapter"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = MangaDirectory::class,
            parentColumns = ["id"],
            childColumns = ["manga_directory_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChapterArchive(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "chapter")
    val chapter: String,

    @ColumnInfo(name = "chapter_path")
    val path: String,

    @ColumnInfo(name = "chapter_sort")
    val chapterSort: String,

    // NOTE: Campo vai manter um hash do arquivo, para se tiver quebrado, ele ignorar no frontend
    @ColumnInfo(name = "checksum")
    val checksum: String? = null,

    @ColumnInfo(name = "fast_hash")
    val fastHash: String? = null,

    @ColumnInfo(name = "manga_directory_fk")
    val folderPathFk: Long,
)
