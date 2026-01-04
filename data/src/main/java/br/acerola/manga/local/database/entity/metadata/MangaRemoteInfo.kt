package br.acerola.manga.local.database.entity.metadata

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.manga.local.database.entity.metadata.relationship.Author
import br.acerola.manga.local.database.entity.metadata.relationship.Cover
import br.acerola.manga.local.database.entity.metadata.relationship.Genre

@Entity(
    tableName = "manga_remote_info",
    foreignKeys = [
        ForeignKey(
            entity = Author::class,
            parentColumns = ["id"],
            childColumns = ["manga_author_fk"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Genre::class,
            parentColumns = ["id"],
            childColumns = ["manga_genre_fk"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Cover::class,
            parentColumns = ["id"],
            childColumns = ["manga_cover_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["title", "manga_author_fk", "manga_genre_fk", "manga_cover_fk"],
            unique = true
        ),
        Index(value = ["mirror_id"], unique = true),
        Index(value = ["manga_author_fk"]),
        Index(value = ["manga_genre_fk"]),
        Index(value = ["manga_cover_fk"])
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
    val mirrorId: String,

    @ColumnInfo(name = "manga_author_fk")
    val mangaAuthorFk: Long?,

    @ColumnInfo(name = "manga_genre_fk")
    val mangaGenreFk: Long?,

    @ColumnInfo(name = "manga_cover_fk")
    val mangaCoverFk: Long?,
)
