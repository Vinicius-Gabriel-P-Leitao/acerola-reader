package br.acerola.manga.local.entity.metadata.relationship

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.manga.local.entity.metadata.MangaMetadata

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
    foreignKeys = [
        ForeignKey(
            entity = MangaMetadata::class,
            parentColumns = ["id"],
            childColumns = ["manga_metadata_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["name", "manga_metadata_fk"], unique = true),
        Index(value = ["manga_metadata_fk"])
    ]
)
data class Author(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: TypeAuthor,

    @ColumnInfo(name = "manga_metadata_fk")
    val mangaRemoteInfoFk: Long
)
