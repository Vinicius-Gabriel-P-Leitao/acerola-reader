package br.acerola.manga.local.entity.metadata.source

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.manga.local.entity.metadata.MangaMetadata

@Entity(
    tableName = "mangadex_source",
    foreignKeys = [
        ForeignKey(
            entity = MangaMetadata::class,
            parentColumns = ["id"],
            childColumns = ["manga_metadata_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["manga_metadata_fk"], unique = true)
    ]
)
data class MangadexSource(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "mangadex_id")
    val mangadexId: String,

    @ColumnInfo(name = "anilist_id")
    val anilistId: String?,

    @ColumnInfo(name = "amazon_url")
    val amazonUrl: String?,

    @ColumnInfo(name = "ebookjapan_url")
    val ebookjapanUrl: String?,

    @ColumnInfo(name = "raw_url")
    val rawUrl: String?,

    @ColumnInfo(name = "engtl_url")
    val engtlUrl: String?,

    @ColumnInfo(name = "manga_metadata_fk")
    val mangaRemoteInfoFk: Long
)
