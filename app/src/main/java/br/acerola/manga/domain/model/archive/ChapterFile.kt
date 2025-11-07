package br.acerola.manga.domain.model.archive

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapter_file",
    indices = [
        Index(value = ["folder_path_fk", "chapter"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = MangaFolder::class,
            parentColumns = ["id"],
            childColumns = ["folder_path_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChapterFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "chapter")
    val chapter: String,

    @ColumnInfo(name = "chapter_path")
    val path: String,

    @ColumnInfo(name = "folder_path_fk")
    val folderPathFk: Long,
)
