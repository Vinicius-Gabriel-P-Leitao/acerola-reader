package br.acerola.manga.domain.model.metadata.gender

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gender",
    indices = [
        Index(value = ["mirror_id"], unique = true),
        Index(value = ["gender"], unique = true)
    ]
)
data class Gender(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "gender")
    val gender: String,

    @ColumnInfo(name = "mirror_id")
    val mirrorId: String
)
