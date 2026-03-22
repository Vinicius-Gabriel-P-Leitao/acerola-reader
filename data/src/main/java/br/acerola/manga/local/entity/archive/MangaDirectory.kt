package br.acerola.manga.local.entity.archive

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "manga_directory",
    indices = [Index(value = ["name"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = ChapterTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapter_template_fk"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class MangaDirectory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "cover")
    val cover: String?,

    @ColumnInfo(name = "banner")
    val banner: String?,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long,

    @ColumnInfo(name = "chapter_template_fk")
    val chapterTemplateFk: Long?,

    @ColumnInfo(name = "external_sync_enabled")
    val externalSyncEnabled: Boolean = true,
)
