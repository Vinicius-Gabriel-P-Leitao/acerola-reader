package br.acerola.comic.local.entity.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.entity.archive.ComicDirectory

@Entity(
    tableName = "reading_history",
    foreignKeys = [
        ForeignKey(
            entity = ComicDirectory::class,
            parentColumns = ["id"],
            childColumns = ["comic_directory_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ChapterArchive::class,
            parentColumns = ["id"],
            childColumns = ["chapter_archive_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ReadingHistory(
    @PrimaryKey
    @ColumnInfo(name = "comic_directory_id")
    val mangaDirectoryId: Long,
    @ColumnInfo(name = "chapter_archive_id")
    val chapterArchiveId: Long,
    @ColumnInfo(name = "last_page")
    val lastPage: Int,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
