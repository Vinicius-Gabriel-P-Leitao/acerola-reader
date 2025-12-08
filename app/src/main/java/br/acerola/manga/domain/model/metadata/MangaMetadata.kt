package br.acerola.manga.domain.model.metadata

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.manga.domain.model.metadata.author.Author
import br.acerola.manga.domain.model.metadata.cover.Cover
import br.acerola.manga.domain.model.metadata.gender.Gender

@Entity(
    tableName = "manga_metadata",
    foreignKeys = [
        ForeignKey(
            entity = Author::class,
            parentColumns = ["id"],
            childColumns = ["manga_author_fk"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Gender::class,
            parentColumns = ["id"],
            childColumns = ["manga_gender_fk"],
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
            value = ["name", "manga_author_fk", "manga_gender_fk", "manga_cover_fk"],
            unique = true
        ),
        Index(value = ["mirror_id"], unique = true),
        Index(value = ["manga_author_fk"]),
        Index(value = ["manga_gender_fk"]),
        Index(value = ["manga_cover_fk"])
    ]
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

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "publication")
    val publication: Int?,

    @ColumnInfo(name = "mirror_id")
    val mirrorId: String,

    @ColumnInfo(name = "manga_author_fk")
    val mangaAuthorFk: Long?,

    @ColumnInfo(name = "manga_gender_fk")
    val mangaGenderFk: Long?,

    @ColumnInfo(name = "manga_cover_fk")
    val mangaCoverFk: Long?,
)
