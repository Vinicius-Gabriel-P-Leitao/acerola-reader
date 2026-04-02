package br.acerola.manga.module.main.search.state

data class DownloadProgress(
    val mangaTitle: String,
    val progress: Int,
    val currentChapterId: String?,
    val currentChapterFileName: String?,
    val totalChapters: Int,
    val isRunning: Boolean = true
)
