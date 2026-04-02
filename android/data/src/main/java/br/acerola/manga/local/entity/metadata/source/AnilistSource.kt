package br.acerola.manga.local.entity.metadata.source

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.manga.local.entity.metadata.MangaMetadata

@Entity(
    tableName = "anilist_source",
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
data class AnilistSource(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "anilist_id")
    val anilistId: Int,

    @ColumnInfo(name = "average_score")
    val averageScore: Int?,

    @ColumnInfo(name = "popularity")
    val popularity: Int?,

    @ColumnInfo(name = "trending")
    val trending: Int?,

    @ColumnInfo(name = "cover_image")
    val coverImage: String?,

    @ColumnInfo(name = "banner_image")
    val bannerImage: String?,

    @ColumnInfo(name = "manga_metadata_fk")
    val mangaRemoteInfoFk: Long
)
