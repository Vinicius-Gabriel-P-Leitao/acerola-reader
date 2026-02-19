package br.acerola.manga.local.database.entity.metadata

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.manga.local.database.entity.archive.MangaDirectory

@Entity(
    tableName = "manga_remote_info",
    indices = [
        Index(value = ["mirror_id"], unique = true),
        Index(value = ["manga_directory_fk"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = MangaDirectory::class,
            parentColumns = ["id"],
            childColumns = ["manga_directory_fk"],
            onDelete = ForeignKey.CASCADE
        )
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

    @ColumnInfo(name = "metadata_source")
    val metadataSource: MetadataSource = MetadataSource.MANGADEX,

    @ColumnInfo(name = "manga_directory_fk")
    val mangaDirectoryFk: Long? = null
)
