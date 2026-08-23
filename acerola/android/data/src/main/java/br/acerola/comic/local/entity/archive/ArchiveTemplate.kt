package br.acerola.comic.local.entity.archive

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.acerola.comic.util.sort.SortType

@Entity(
    tableName = "archive_template",
    indices = [
        Index(value = ["label"], unique = true),
    ],
)
data class ArchiveTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "label")
    val label: String,
    @ColumnInfo(name = "pattern")
    val pattern: String,
    @ColumnInfo(name = "type")
    val type: SortType,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
    @ColumnInfo(name = "priority")
    val priority: Int = 0,
)
