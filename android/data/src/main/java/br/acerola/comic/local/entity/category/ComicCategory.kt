package br.acerola.comic.local.entity.category

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.comic.local.entity.archive.ComicDirectory

@Entity(
    tableName = "comic_category",
    indices = [
        Index(value = ["comic_directory_fk"], unique = true),
        Index(value = ["category_id"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ComicDirectory::class,
            parentColumns = ["id"],
            childColumns = ["comic_directory_fk"],
            onDelete = ForeignKey.Companion.CASCADE,
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.Companion.CASCADE,
        ),
    ],
)
data class ComicCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "comic_directory_fk")
    val comicDirectoryFk: Long,
    // FIXME: Trocar _id por _fk
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
)
