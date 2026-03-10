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
            childColumns = ["manga_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChapterArchive::class,
            parentColumns = ["id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReadingHistory(
    @PrimaryKey
    @ColumnInfo(name = "manga_id")
    val mangaId: Long,

    @ColumnInfo(name = "chapter_id")
    val chapterId: Long,

    @ColumnInfo(name = "last_page")
    val lastPage: Int,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
