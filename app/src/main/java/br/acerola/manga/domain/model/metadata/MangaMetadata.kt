package br.acerola.manga.domain.model.metadata

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "manga_metadata"
)
data class MangaMetadata(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "romanji")
    val romanji: String,

    // TODO: Criar um enum para esses gêneros
    @ColumnInfo(name = "gender")
    val gender: String,

    @ColumnInfo(name = "publication")
    val publication: Int,

    @ColumnInfo(name = "author")
    val author: String,
)
