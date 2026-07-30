package br.acerola.comic.local.entity.metadata

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import br.acerola.comic.local.entity.archive.ChapterArchive

@Entity(
    tableName = "chapter_metadata",
    foreignKeys = [
        ForeignKey(
            entity = ComicMetadata::class,
            parentColumns = ["id"],
            childColumns = ["comic_metadata_fk"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ChapterArchive::class,
            parentColumns = ["id"],
            childColumns = ["chapter_archive_fk"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("comic_metadata_fk"),
        Index("chapter_archive_fk"),
        Index(value = ["chapter", "comic_metadata_fk"], unique = true),
    ],
)
data class ChapterMetadata(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "title")
    val title: String?,
    @ColumnInfo(name = "chapter")
    val chapter: String,
    @ColumnInfo(name = "page_count")
    val pageCount: Int? = null,
    @ColumnInfo(name = "scanlation")
    val scanlation: String? = null,
    @ColumnInfo(name = "comic_metadata_fk")
    val comicRemoteInfoFk: Long,
    @ColumnInfo(name = "chapter_archive_fk")
    val chapterArchiveFk: Long? = null,
)
