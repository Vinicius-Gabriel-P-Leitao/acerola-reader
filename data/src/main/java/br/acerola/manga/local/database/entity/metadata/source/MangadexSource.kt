package br.acerola.manga.local.database.entity.metadata.source

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.manga.local.database.entity.metadata.MangaRemoteInfo

@Entity(
    tableName = "mangadex_source",
    foreignKeys = [
        ForeignKey(
            entity = MangaRemoteInfo::class,
            parentColumns = ["id"],
            childColumns = ["manga_remote_info_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["manga_remote_info_fk"], unique = true)
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

    @ColumnInfo(name = "manga_remote_info_fk")
    val mangaRemoteInfoFk: Long
)
