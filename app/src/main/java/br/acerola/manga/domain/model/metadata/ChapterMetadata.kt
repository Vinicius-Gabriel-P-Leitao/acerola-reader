package br.acerola.manga.domain.model.metadata

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
    indices = [Index("manga_metadata_fk")]
)
data class ChapterMetadata(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "chapter")
    val chapter: String,

    @ColumnInfo(name = "title")
    val title: String?,

    @ColumnInfo(name = "release_date")
    val releaseDate: Long?,

    @ColumnInfo(name = "summary")
    val summary: String?,

    @ColumnInfo(name = "page_count")
    val pageCount: Int? = null,

    @ColumnInfo(name = "scanlator")
    val scanlator: String? = null,

    @ColumnInfo(name = "manga_metadata_fk")
    val mangaMetadataFk: Long,
)
