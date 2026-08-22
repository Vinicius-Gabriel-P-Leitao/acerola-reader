package br.acerola.comic.local.entity.relation

import androidx.room.ColumnInfo

data class LibrarySummaryRow(
    @ColumnInfo(name = "comic_name")
    val comicName: String,
    @ColumnInfo(name = "chapter_count")
    val chapterCount: Int,
    // Reaproveita `comic_directory.last_modified` — sem hash novo. O peer compara contra a
    // versão já cacheada localmente pra decidir se precisa buscar uma capa nova via
    // `acerola/browse-cover/1`.
    @ColumnInfo(name = "cover_version")
    val coverVersion: Long,
)
