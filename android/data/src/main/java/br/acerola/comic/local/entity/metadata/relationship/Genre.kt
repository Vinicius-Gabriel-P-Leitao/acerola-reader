package br.acerola.comic.local.entity.metadata.relationship

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.comic.local.entity.metadata.ComicMetadata

@Entity(
    tableName = "genre",
    foreignKeys = [
        ForeignKey(
            entity = ComicMetadata::class,
            parentColumns = ["id"],
            childColumns = ["comic_metadata_fk"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["genre", "comic_metadata_fk"], unique = true),
        Index(value = ["comic_metadata_fk"]),
    ],
)
data class Genre(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "genre")
    val genre: String,
    @ColumnInfo(name = "comic_metadata_fk")
    val comicRemoteInfoFk: Long,
)
