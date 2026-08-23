package br.acerola.comic.local.entity.archive

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "volume_archive",
    indices = [
        Index(value = ["comic_directory_fk", "volume_sort"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ComicDirectory::class,
            parentColumns = ["id"],
            childColumns = ["comic_directory_fk"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class VolumeArchive(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "path")
    val path: String,
    @ColumnInfo(name = "volume_sort")
    val volumeSort: String,
    @ColumnInfo(name = "is_special", defaultValue = "0")
    val isSpecial: Boolean = false,
    @ColumnInfo(name = "cover")
    val cover: String? = null,
    @ColumnInfo(name = "banner")
    val banner: String? = null,
    @ColumnInfo(name = "comic_directory_fk")
    val comicDirectoryFk: Long,
    @ColumnInfo(name = "last_modified", defaultValue = "0")
    val lastModified: Long = 0,
)
