package br.acerola.manga.domain.service.archive

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.domain.builder.ArchiveBuilder
import br.acerola.manga.domain.database.dao.archive.ChapterFileDao
import br.acerola.manga.domain.database.dao.archive.MangaFolderDao
import br.acerola.manga.domain.mapper.toDto
import br.acerola.manga.domain.model.archive.ChapterFile
import br.acerola.manga.domain.model.archive.MangaFolder
import br.acerola.manga.shared.config.FileExtension
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
import java.util.concurrent.atomic.AtomicInteger

class ArchiveMangaService(
    private val context: Context, private val folderDao: MangaFolderDao, private val chapterDao: ChapterFileDao
) {
    private val _progress = MutableStateFlow(value = -1)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val CHUNK_SIZE = 50
    private val PROGRESS_THRESHOLD = 5

    suspend fun syncFolders(baseUri: Uri) = withContext(context = Dispatchers.IO) {
        val folders: List<MangaFolder> = ArchiveBuilder.buildLibrary(context, rootUri = baseUri)
        val existingFolders: List<MangaFolder> = folderDao.getAllMangasFolders().firstOrNull() ?: emptyList()

        if (folders.isEmpty() && existingFolders.isEmpty()) {
            _progress.value = -1
            return@withContext
        }

        val existingFoldersMap: Map<String, MangaFolder> = existingFolders.associateBy { it.path }

        val foldersToProcess = folders.filter { folder ->
            val existing: MangaFolder? = existingFoldersMap[folder.path]

            when {
                existing == null -> true
                existing.lastModified < folder.lastModified -> true
                existing.cover != folder.cover || existing.banner != folder.banner -> true
                else -> false
            }
        }

        val currentPaths = folders.map { it.path }.toSet()
        val removedFolders = existingFolders.filter { it.path !in currentPaths }

        if (removedFolders.isNotEmpty()) {
            removedFolders.forEach { folder ->
                // NOTE: Ele deleta os capitulos de forma recursiva, joga pro sqlite
                folderDao.deleteMangaFolder(manga = folder)
            }
        }

        processFolderList(foldersToProcess, existingFolders)
    }

    suspend fun rescanAllFolders(baseUri: Uri) = withContext(context = Dispatchers.IO) {
        val foldersToProcess: List<MangaFolder> = ArchiveBuilder.buildLibrary(context, rootUri = baseUri)
        if (foldersToProcess.isEmpty()) {
            _progress.value = -1
            return@withContext
        }

        val existingFolders = folderDao.getAllMangasFolders().firstOrNull() ?: emptyList()
        processFolderList(foldersToProcess, existingFolders)
    }

    suspend fun scanAndSyncChapters(folderId: Long) = withContext(context = Dispatchers.IO) {
        val folder = folderDao.getMangaFolderById(mangaId = folderId) ?: return@withContext
        val folderDoc = DocumentFile.fromTreeUri(context, folder.path.toUri()) ?: return@withContext

        val chaptersExist = chapterDao.countChaptersByFolder(folderId) > 0

        if (chaptersExist && folder.lastModified >= folderDoc.lastModified()) {
            return@withContext
        }

        val chapterFiles = folderDoc.listFiles().filter { it.isFile }.filter { file ->
            FileExtension.isSupported(ext = file.name)
        }

        chapterDao.deleteChaptersByFolderId(folderId)

        val chapters = chapterFiles.map { file ->
            ChapterFile(
                chapter = file.name ?: "Unknown", path = file.uri.toString(), folderPathFk = folderId
            )
        }

        if (chapters.isNotEmpty()) {
            chapterDao.insertAll(chapters)
        }

        if (folder.lastModified < folderDoc.lastModified()) {
            folderDao.updateMangaFolder(manga = folder.copy(lastModified = folderDoc.lastModified()))
        }
    }

    fun getAllFolders(): StateFlow<List<MangaFolderDto>> {
        return folderDao.getAllMangasFolders().combine(flow = chapterDao.getAllChapterFiles()) { folders, chapters ->
            folders.map { folder ->
                folder.toDto(chapters.filter { it.folderPathFk == folder.id })
            }
        }.stateIn(
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    }

    fun getChaptersByFolder(folderId: Long): StateFlow<List<ChapterFileDto>> {
        return chapterDao.getChaptersByFolder(folderId).map { list ->
            list.map { it.toDto() }
        }.stateIn(
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    }

    private suspend fun processFolderList(foldersToProcess: List<MangaFolder>, existingFolders: List<MangaFolder>) {
        if (foldersToProcess.isEmpty()) {
            _progress.value = -1
            return
        }

        val total = foldersToProcess.size
        val showProgress = total >= PROGRESS_THRESHOLD

        if (!showProgress) {
            foldersToProcess.chunked(CHUNK_SIZE).forEach { batch ->
                coroutineScope {
                    batch.map { folder ->
                        async(context = Dispatchers.IO) {
                            upsertFolder(folder, existingFolders)
                        }
                    }.awaitAll()
                }
            }

            delay(timeMillis = 250)
            _progress.value = -1
            return
        }

        val processed = AtomicInteger(0)
        _progress.value = 0
        foldersToProcess.chunked(CHUNK_SIZE).forEach { batch ->
            coroutineScope {
                batch.map { folder ->
                    async(context = Dispatchers.IO) {
                        try {
                            upsertFolder(folder, existingFolders)
                        } finally {
                            val current = processed.incrementAndGet()
                            _progress.value = ((current.toFloat() / total) * 100).toInt()
                        }
                    }
                }.awaitAll()
            }
        }
        _progress.value = 100

        delay(timeMillis = 250)
        _progress.value = -1
    }

    private suspend fun upsertFolder(folder: MangaFolder, existingFolders: List<MangaFolder>) {
        val existing = existingFolders.find { it.path == folder.path }

        if (existing != null) {
            folderDao.updateMangaFolder(manga = folder.copy(id = existing.id))
            return
        }

        folderDao.insertMangaFolder(manga = folder)
    }
}
