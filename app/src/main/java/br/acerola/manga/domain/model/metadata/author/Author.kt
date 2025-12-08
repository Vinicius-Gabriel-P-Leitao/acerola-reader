package br.acerola.manga.domain.model.metadata.author

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TypeAuthor(val type: String) {
    AUTHOR(type = "author"),
    ARTIST(type = "artist");

    companion object {
        fun getByType(type: String): TypeAuthor {
            return entries.find { it.type == type } ?: AUTHOR
        }
    }
}

@Entity(
    tableName = "author",
    indices = [
        Index(value = ["mirror_id"], unique = true),
        Index(value = ["name"], unique = false)
    ]
)
data class Author(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: TypeAuthor,

    @ColumnInfo(name = "mirror_id")
    val mirrorId: String
)
