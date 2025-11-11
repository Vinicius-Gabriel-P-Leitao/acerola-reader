package br.acerola.manga.domain.service.library

import android.net.Uri
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.shared.dto.archive.MangaFolderDto
import kotlinx.coroutines.flow.StateFlow

interface LibraryPort {
    val progress: StateFlow<Int>
    fun getAllMangas(): StateFlow<List<MangaFolderDto>>
    fun getChapters(mangaId: Long): StateFlow<List<ChapterFileDto>>

    suspend fun syncMangas(baseUri: Uri)
    suspend fun rescanMangas(baseUri: Uri)
    suspend fun deepRescanLibrary(baseUri: Uri)
    suspend fun rescanChaptersByManga(mangaId: Long)
}