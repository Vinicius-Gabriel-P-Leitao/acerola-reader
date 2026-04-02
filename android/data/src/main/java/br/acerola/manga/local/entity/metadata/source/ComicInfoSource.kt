package br.acerola.manga.local.entity.metadata.source

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.manga.local.entity.metadata.MangaMetadata

@Entity(
    tableName = "comic_info_source",
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
data class ComicInfoSource(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "local_hash")
    val localHash: String,

    @ColumnInfo(name = "manga_metadata_fk")
    val mangaRemoteInfoFk: Long
)
