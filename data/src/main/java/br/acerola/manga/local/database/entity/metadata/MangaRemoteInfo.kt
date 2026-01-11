package br.acerola.manga.local.database.entity.metadata

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "manga_remote_info",
    indices = [
        Index(value = ["mirror_id"], unique = true)
    ]
)
data class MangaRemoteInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "romanji")
    val romanji: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "publication")
    val publication: Int?,

    @ColumnInfo(name = "mirror_id")
    val mirrorId: String
)
