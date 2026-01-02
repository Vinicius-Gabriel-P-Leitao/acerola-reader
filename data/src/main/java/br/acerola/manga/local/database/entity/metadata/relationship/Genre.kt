package br.acerola.manga.local.database.entity.metadata.relationship

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "genre",
    indices = [
        Index(value = ["mirror_id"], unique = true),
        Index(value = ["genre"], unique = true)
    ]
)
data class Genre(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "genre")
    val genre: String,

    @ColumnInfo(name = "mirror_id")
    val mirrorId: String
)
