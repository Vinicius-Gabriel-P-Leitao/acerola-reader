package br.acerola.manga.local.entity.metadata

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapter_metadata",
    foreignKeys = [
        ForeignKey(
            entity = MangaMetadata::class,
            parentColumns = ["id"],
            childColumns = ["manga_metadata_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("manga_metadata_fk"),
        Index(value = ["chapter", "manga_metadata_fk"], unique = true)
    ]
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

    @ColumnInfo(name = "manga_metadata_fk")
    val mangaRemoteInfoFk: Long,
)
