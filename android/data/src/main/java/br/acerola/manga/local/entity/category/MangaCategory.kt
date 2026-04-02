package br.acerola.manga.local.entity.category

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.manga.local.entity.archive.MangaDirectory

@Entity(
    tableName = "manga_category",
    indices = [
        Index(value = ["manga_directory_fk"], unique = true),
        Index(value = ["category_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = MangaDirectory::class,
            parentColumns = ["id"],
            childColumns = ["manga_directory_fk"],
            onDelete = ForeignKey.Companion.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ]
)
data class MangaCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "manga_directory_fk")
    val mangaDirectoryFk: Long,

    @ColumnInfo(name = "category_id")
    val categoryId: Long
)