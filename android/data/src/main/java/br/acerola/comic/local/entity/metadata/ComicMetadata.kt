package br.acerola.comic.local.entity.metadata

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.comic.local.entity.archive.ComicDirectory

@Entity(
    tableName = "comic_metadata",
    indices = [
        Index(value = ["comic_directory_fk"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = ComicDirectory::class,
            parentColumns = ["id"],
            childColumns = ["comic_directory_fk"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ComicMetadata(
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

    @ColumnInfo(name = "sync_source")
    val syncSource: String? = null,

    @ColumnInfo(name = "has_comic_info")
    val hasComicInfo: Boolean = false,

    @ColumnInfo(name = "comic_directory_fk")
    val mangaDirectoryFk: Long? = null,
)
