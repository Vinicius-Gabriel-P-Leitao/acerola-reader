package br.acerola.comic.local.entity.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import br.acerola.comic.local.entity.archive.ChapterArchive
import br.acerola.comic.local.entity.archive.ComicDirectory

@Entity(
    tableName = "chapter_read",
    primaryKeys = ["comic_directory_id", "chapter_archive_id"],
    foreignKeys = [
        ForeignKey(
            entity = ComicDirectory::class,
            parentColumns = ["id"],
            childColumns = ["comic_directory_id"],
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
data class ChapterRead(
    @ColumnInfo(name = "comic_directory_id")
    val mangaDirectoryId: Long,
    
    @ColumnInfo(name = "chapter_archive_id")
    val chapterArchiveId: Long,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
