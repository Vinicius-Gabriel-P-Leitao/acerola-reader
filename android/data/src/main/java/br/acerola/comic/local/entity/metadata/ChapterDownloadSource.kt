package br.acerola.comic.local.entity.metadata

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapter_page",
    foreignKeys = [
        ForeignKey(
            entity = ChapterMetadata::class,
            parentColumns = ["id"],
            childColumns = ["chapter_fk"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("chapter_fk"),
        Index(value = ["chapter_fk", "page_number"], unique = true),
    ],
)
data class ChapterDownloadSource(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "page_number")
    val pageNumber: Int,
    @ColumnInfo(name = "image_url")
    val imageUrl: String,
    @ColumnInfo(name = "downloaded")
    val downloaded: Boolean = false,
    @ColumnInfo(name = "chapter_fk")
    val chapterFk: Long,
)
