package br.acerola.manga.local.database.entity.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import br.acerola.manga.local.database.entity.archive.ChapterArchive
import br.acerola.manga.local.database.entity.archive.MangaDirectory

@Entity(
    tableName = "reading_history",
    foreignKeys = [
        ForeignKey(
            entity = MangaDirectory::class,
            parentColumns = ["id"],
            childColumns = ["manga_directory_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChapterArchive::class,
            parentColumns = ["id"],
            childColumns = ["chapter_archive_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReadingHistory(
    @PrimaryKey
    @ColumnInfo(name = "manga_directory_id")
    val mangaDirectoryId: Long,

    @ColumnInfo(name = "chapter_archive_id")
    val chapterArchiveId: Long,

    @ColumnInfo(name = "last_page")
    val lastPage: Int,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
