package br.acerola.comic.local.entity.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import br.acerola.comic.local.entity.archive.ComicDirectory

// TODO: Adicionar campo de _fk do volume
@Entity(
    tableName = "reading_history",
    foreignKeys = [
        ForeignKey(
            entity = ComicDirectory::class,
            parentColumns = ["id"],
            childColumns = ["comic_directory_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ReadingHistory(
    @PrimaryKey
    // FIXME: Trocar _id por _fk
    @ColumnInfo(name = "comic_directory_id")
    val comicDirectoryId: Long,
    @ColumnInfo(name = "chapter_sort")
    val chapterSort: String,
    // FIXME: Trocar _id por _fk
    @ColumnInfo(name = "chapter_archive_id")
    val chapterArchiveId: Long? = null,
    @ColumnInfo(name = "last_page")
    val lastPage: Int,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
