package br.acerola.manga.domain.model.metadata

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "manga_metadata",
    indices = [Index(value = ["name"], unique = true)]
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

    // TODO: Criar uma tabela só pra isso
    @ColumnInfo(name = "gender")
    val gender: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "publication")
    val publication: Int,

    // TODO: Criar uma tabela só pra isso
    @ColumnInfo(name = "author")
    val author: String,

    // TODO: Criar uma tabela só pra isso
    // @ColumnInfo(name = "cover")
    // val cover: String,
)
