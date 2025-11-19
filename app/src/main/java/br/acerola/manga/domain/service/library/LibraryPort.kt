package br.acerola.manga.domain.service.library

import android.net.Uri
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.shared.dto.archive.ChapterPageDto
import br.acerola.manga.shared.dto.archive.MangaFolderDto
import kotlinx.coroutines.flow.StateFlow

interface LibraryPort {

    val progress: StateFlow<Int>

    suspend fun syncMangas(baseUri: Uri)
    suspend fun rescanMangas(baseUri: Uri)
    suspend fun deepRescanLibrary(baseUri: Uri)

    interface MangaOperations {
        fun loadMangas(): StateFlow<List<MangaFolderDto>>
        suspend fun rescanChaptersByManga(mangaId: Long)
    }

    interface ChapterOperations {
        fun loadChapterByManga(mangaId: Long): StateFlow<List<ChapterFileDto>>
        suspend fun loadNextPage(folderId: Long, total: Int, page: Int, pageSize: Int = 20): ChapterPageDto
    }
}
