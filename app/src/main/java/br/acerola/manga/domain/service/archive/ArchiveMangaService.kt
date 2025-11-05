package br.acerola.manga.domain.service.archive

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.domain.builder.ArchiveBuilder
import br.acerola.manga.domain.mapper.toDto
import br.acerola.manga.domain.database.dao.archive.ChapterFileDao
import br.acerola.manga.domain.database.dao.archive.MangaFolderDao
import br.acerola.manga.domain.model.archive.ChapterFile
import br.acerola.manga.domain.model.archive.MangaFolder
import br.acerola.manga.shared.config.FileExtensions
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.shared.dto.archive.MangaFolderDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class ArchiveMangaService(
    private val context: Context, private val folderDao: MangaFolderDao, private val chapterDao: ChapterFileDao
) {
    private val _progress = MutableStateFlow(value = -1)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val CHUNK_SIZE = 50
    private val CONCURRENCY = 4

    suspend fun indexLibrary(baseUri: Uri) = withContext(context = Dispatchers.IO) {
        val folders = ArchiveBuilder.buildLibrary(context, rootUri = baseUri)
        if (folders.isEmpty()) {
            _progress.value = -1
            return@withContext
        }

        val existingFolders = folderDao.getAllMangasFolders().firstOrNull() ?: emptyList()
        val total = folders.size
        var processed = 0

        folders.chunked(CHUNK_SIZE).forEach { batch ->
            coroutineScope {
                batch.map { folder ->
                    async(context = Dispatchers.IO) {
                        processSingleFolder(folder, existingFolders)
                    }
                }.chunked(CONCURRENCY).flatMap { it.awaitAll() }

                processed += batch.size
                _progress.value = (processed * 100 / total).coerceIn(0, 100)
                yield()
            }
        }

        _progress.value = 100
        delay(timeMillis = 250)
        _progress.value = -1
    }

    private suspend fun processSingleFolder(folder: MangaFolder, existingFolders: List<MangaFolder>) {
        val existing = existingFolders.find { it.path == folder.path }
        if (existing != null && existing.lastModified == folder.lastModified) return

        val folderId: Long = folderDao.insertMangaFolder(manga = folder)
        val folderDoc = DocumentFile.fromTreeUri(context, folder.path.toUri()) ?: return

        val files = folderDoc.listFiles().asSequence().filter { it.isFile }.filter { file ->
            FileExtensions.comicBookFormats.any { ext ->
                file.name?.endsWith(ext, ignoreCase = true) == true
            }
        }.toList()

        val chapters = files.chunked(CHUNK_SIZE).flatMap { chunk ->
            chunk.map { file ->
                ChapterFile(
                    chapter = file.name ?: "Unknown", path = file.uri.toString(), folderPathFk = folderId
                )
            }
        }

        if (chapters.isNotEmpty()) {
            chapterDao.insertAll(chapters)
        }
    }

    fun getAllFoldersDto(): StateFlow<List<MangaFolderDto>> {
        return folderDao.getAllMangasFolders().combine(chapterDao.getAllChapterFiles()) { folders, chapters ->
            folders.map { folder ->
                folder.toDto(chapters.filter { it.folderPathFk == folder.id })
            }
        }.stateIn(
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    }

    fun getChaptersByFolderDto(folderId: Long): StateFlow<List<ChapterFileDto>> {
        return chapterDao.getChaptersByFolder(folderId).map { list ->
            list.map { it.toDto() }
        }.stateIn(
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    }
}
