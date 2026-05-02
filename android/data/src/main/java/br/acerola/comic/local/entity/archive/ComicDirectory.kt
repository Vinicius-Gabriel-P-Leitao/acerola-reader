package br.acerola.comic.local.entity.archive

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "comic_directory",
    indices = [Index(value = ["name"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = ArchiveTemplate::class,
            parentColumns = ["id"],
            childColumns = ["archive_template_fk"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class ComicDirectory(
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
    @ColumnInfo(name = "archive_template_fk")
    val archiveTemplateFk: Long?,
    @ColumnInfo(name = "external_sync_enabled")
    val externalSyncEnabled: Boolean = true,
    @ColumnInfo(name = "hidden", defaultValue = "0")
    val hidden: Boolean = false,
)
