package br.acerola.comic.local.entity.metadata.relationship

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.comic.local.entity.metadata.ComicMetadata

@Entity(
    tableName = "banner",
    foreignKeys = [
        ForeignKey(
            entity = ComicMetadata::class,
            parentColumns = ["id"],
            childColumns = ["comic_metadata_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["file_name", "comic_metadata_fk"], unique = true),
        Index(value = ["comic_metadata_fk"])
    ]
)
data class Banner(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "comic_metadata_fk")
    val mangaRemoteInfoFk: Long
)
