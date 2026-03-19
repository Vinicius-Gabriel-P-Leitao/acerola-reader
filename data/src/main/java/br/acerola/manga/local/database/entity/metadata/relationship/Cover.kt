package br.acerola.manga.local.database.entity.metadata.relationship

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.manga.local.database.entity.metadata.MangaRemoteInfo


@Entity(
    tableName = "cover",
    foreignKeys = [
        ForeignKey(
            entity = MangaRemoteInfo::class,
            parentColumns = ["id"],
            childColumns = ["manga_remote_info_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["file_name", "manga_remote_info_fk"], unique = true),
        Index(value = ["manga_remote_info_fk"])
    ]
)
data class Cover(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "manga_remote_info_fk")
    val mangaRemoteInfoFk: Long
)
