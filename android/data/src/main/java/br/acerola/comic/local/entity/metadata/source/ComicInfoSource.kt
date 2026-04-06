package br.acerola.comic.local.entity.metadata.source

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.comic.local.entity.metadata.ComicMetadata

@Entity(
    tableName = "comic_info_source",
    foreignKeys = [
        ForeignKey(
            entity = ComicMetadata::class,
            parentColumns = ["id"],
            childColumns = ["comic_metadata_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["comic_metadata_fk"], unique = true)
    ]
)
data class ComicInfoSource(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "local_hash")
    val localHash: String,

    @ColumnInfo(name = "comic_metadata_fk")
    val mangaRemoteInfoFk: Long
)
