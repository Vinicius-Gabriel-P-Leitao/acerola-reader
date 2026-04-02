package br.acerola.manga.local.entity.metadata.relationship

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.manga.local.entity.metadata.MangaMetadata

@Entity(
    tableName = "genre",
    foreignKeys = [
        ForeignKey(
            entity = MangaMetadata::class,
            parentColumns = ["id"],
            childColumns = ["manga_metadata_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["genre", "manga_metadata_fk"], unique = true),
        Index(value = ["manga_metadata_fk"])
    ]
)
data class Genre(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "genre")
    val genre: String,

    @ColumnInfo(name = "manga_metadata_fk")
    val mangaRemoteInfoFk: Long
)
