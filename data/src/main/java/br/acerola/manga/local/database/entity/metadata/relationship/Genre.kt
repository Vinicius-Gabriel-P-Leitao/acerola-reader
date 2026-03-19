package br.acerola.manga.local.database.entity.metadata.relationship

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.manga.local.database.entity.metadata.MangaRemoteInfo

@Entity(
    tableName = "genre",
    foreignKeys = [
        ForeignKey(
            entity = MangaRemoteInfo::class,
            parentColumns = ["id"],
            childColumns = ["manga_remote_info_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["mirror_id"], unique = false),
        Index(value = ["genre"], unique = false),
        Index(value = ["manga_remote_info_fk"])
    ]
)
data class Genre(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "genre")
    val genre: String,

    // Remover os mirror_id e ir para a tabela de origem.
    @ColumnInfo(name = "mirror_id")
    val mirrorId: String,

    @ColumnInfo(name = "manga_remote_info_fk")
    val mangaRemoteInfoFk: Long
)
