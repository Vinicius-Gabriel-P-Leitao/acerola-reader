package br.acerola.comic.local.entity.archive

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapter_template",
    indices = [
        Index(value = ["label"], unique = true),
        // Removido unique de pattern para permitir presets com labels diferentes e mesmos patterns
    ],
)
data class ChapterTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "label")
    val label: String,
    @ColumnInfo(name = "pattern")
    val pattern: String,
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
    @ColumnInfo(name = "priority")
    val priority: Int = 0,
)
