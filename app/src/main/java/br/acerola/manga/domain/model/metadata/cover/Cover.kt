package br.acerola.manga.domain.model.metadata.cover

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "cover",
    indices = [
        Index(value = ["mirror_id"], unique = true)
    ]
)
data class Cover(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "mirror_id")
    val mirrorId: String
)
