package br.acerola.manga.local.database.entity.metadata.relationship

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.manga.local.database.entity.metadata.MangaRemoteInfo

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
            entity = MangaRemoteInfo::class,
            parentColumns = ["id"],
            childColumns = ["manga_remote_info_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["mirror_id"], unique = false),
        Index(value = ["name"], unique = false),
        Index(value = ["manga_remote_info_fk"])
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
    val mirrorId: String,

    @ColumnInfo(name = "manga_remote_info_fk")
    val mangaRemoteInfoFk: Long
)
