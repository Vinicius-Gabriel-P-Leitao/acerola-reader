package br.acerola.comic.local.entity.relation

data class VolumeChapterCount(
    val id: Long,
    val name: String,
    val volumeSort: String,
    val isSpecial: Boolean,
    val cover: String?,
    val banner: String?,
    val chapterCount: Int,
    val lastModified: Long = 0,
)
