package br.acerola.comic.local.entity.relation

import androidx.room.ColumnInfo

data class LibrarySummaryRow(
    @ColumnInfo(name = "comic_name")
    val comicName: String,
    @ColumnInfo(name = "chapter_count")
    val chapterCount: Int,
)
