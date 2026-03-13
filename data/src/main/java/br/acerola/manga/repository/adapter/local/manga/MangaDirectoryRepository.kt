package br.acerola.manga.repository.adapter.local.manga

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.flatMap
import br.acerola.manga.config.preference.FileExtension
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.entity.archive.MangaDirectory
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.local.mapper.toMangaDirectoryModel
import br.acerola.manga.repository.di.DirectoryFsOps
import br.acerola.manga.repository.port.ChapterManagementRepository
import br.acerola.manga.repository.port.MangaManagementRepository
import br.acerola.manga.util.ContentQueryHelper
import br.acerola.manga.util.FastFileMetadata
import br.acerola.manga.util.detectTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.PatternSyntaxException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaDirectoryRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val directoryDao: MangaDirectoryDao,
) : MangaManagementRepository<MangaDirectoryDto> {

    private val semaphore = Semaphore(permits = 3)

    /**
     * Injeção de dependência circular resolvida via interface.
     * Usado para operations de capítulos dentro do deepRescan.
     */
    @Inject
    @DirectoryFsOps
    lateinit var mangaDirectoryOps: ChapterManagementRepository<ChapterArchivePageDto>

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    companion object {

        const val CHUNK_SIZE = 50
        const val PROGRESS_THRESHOLD = 5
    }

    /**
     * Reescaneia um único mangá específico.
     * Verifica se a pasta ainda existe e atualiza metadados (capa, banner) se necessário.
     */
    override suspend fun refreshManga(mangaId: Long): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            _isIndexing.value = true
            try {
                Either.catch {
                    val existingManga = directoryDao.getMangaDirectoryById(mangaId) ?: return@catch

                    val folderDoc = DocumentFile.fromSingleUri(context, existingManga.path.toUri())

                    // WARN: Caso a pasta não exista ele só ignora
                    if (folderDoc == null || !folderDoc.isDirectory) return@catch

                    // NOTE: Se não mudou data de modificação, assume atualizado, confia mais no file system doq do user
                    if (existingManga.lastModified >= folderDoc.lastModified()) return@catch

                    val banner = folderDoc.listFiles().firstOrNull { isBanner(name = it.name) }
                    val cover = folderDoc.listFiles().firstOrNull { isCover(name = it.name) }
                    val hasComicInfo = folderDoc.findFile("ComicInfo.xml") != null

                    val firstChapter = folderDoc.listFiles().firstOrNull { file ->
                        file.isFile && FileExtension.isSupported(ext = file.name)
                    }

                    val detectedTemplate = firstChapter?.name?.let {
                        detectTemplate(fileName = it)
                    }

                    val updatedManga = folderDoc.toMangaDirectoryModel(
                        cover, banner, chapterTemplate = detectedTemplate, hasComicInfo = hasComicInfo
                    ).copy(id = existingManga.id)

                    directoryDao.update(entity = updatedManga)
                    
                    // Trigger chapter refresh (falls back to DocumentFile since we don't have the library tree URI here)
                    mangaDirectoryOps.refreshMangaChapters(mangaId = mangaId, baseUri = null)
                }.mapLeft { exception ->
                    when (exception) {
                        is PatternSyntaxException -> LibrarySyncError.MalformedLibrary(cause = exception)
                        is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                        is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                        else -> LibrarySyncError.UnexpectedError(cause = exception)
                    }
                }
            } finally {
                _isIndexing.value = false
            }
        }

    /**
     * Sincroniza a biblioteca local de mangás com o diretório selecionado.
     */
    override suspend fun incrementalScan(baseUri: Uri?): Either<LibrarySyncError, Unit> {
        _isIndexing.value = true
        try {
            return withContext(context = Dispatchers.IO) {
                Either.catch {
                    if (baseUri === null) return@catch

                    val discoveredFolders: List<MangaDirectory> = buildLibrary(context, rootUri = baseUri)
                    val databaseFolders: List<MangaDirectory> =
                        directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()

                    if (discoveredFolders.isEmpty() && databaseFolders.isEmpty()) {
                        _progress.value = -1
                        return@catch
                    }

                    val existingFoldersMap = databaseFolders.associateBy { normalizeName(it.name) }
                    val foldersMap = discoveredFolders.associateBy { normalizeName(it.name) }

                    val foldersToProcess = discoveredFolders.filter { folder ->
                        val normalizedName = normalizeName(folder.name)
                        val existing = existingFoldersMap[normalizedName]

                        when {
                            existing == null -> true
                            existing.path != folder.path -> true
                            existing.lastModified < folder.lastModified -> true
                            existing.cover != folder.cover || existing.banner != folder.banner -> true
                            else -> false
                        }
                    }

                    val removedFolders = databaseFolders.filter { normalizeName(it.name) !in foldersMap }

                    if (removedFolders.isNotEmpty()) {
                        removedFolders.forEach { folder ->
                            directoryDao.delete(entity = folder)
                        }
                    }

                    processFolderList(foldersToProcess, existingFolders = databaseFolders, baseUri = baseUri)
                }.mapLeft { exception ->
                    when (exception) {
                        is IOException -> LibrarySyncError.DiskIOFailure(path = baseUri.toString(), exception)
                        is PatternSyntaxException -> LibrarySyncError.MalformedLibrary(cause = exception)
                        is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                        is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                        else -> LibrarySyncError.UnexpectedError(cause = exception)
                    }
                }
            }
        } finally {
            _isIndexing.value = false
        }
    }

    override suspend fun refreshLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> {
        _isIndexing.value = true
        try {
            return withContext(context = Dispatchers.IO) {
                Either.catch {
                    if (baseUri === null) return@catch

                    val foldersToProcess: List<MangaDirectory> = buildLibrary(context, rootUri = baseUri)
                    if (foldersToProcess.isEmpty()) {
                        _progress.value = -1
                        return@catch
                    }

                    val existingFolders = directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()
                    processFolderList(foldersToProcess, existingFolders, baseUri = baseUri)
                }.mapLeft { exception ->
                    when (exception) {
                        is IOException -> LibrarySyncError.DiskIOFailure(path = baseUri.toString(), exception)
                        is PatternSyntaxException -> LibrarySyncError.MalformedLibrary(cause = exception)
                        is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                        is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                        else -> LibrarySyncError.UnexpectedError(cause = exception)
                    }
                }
            }
        } finally {
            _isIndexing.value = false
        }
    }

    override suspend fun rebuildLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> {
        _isIndexing.value = true
        try {
            return withContext(context = Dispatchers.IO) {
                refreshLibrary(baseUri).flatMap {
                    Either.catch {
                        val allFolders = directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()

                        if (allFolders.isEmpty()) {
                            _progress.value = -1
                            return@catch
                        }

                        val total = allFolders.size

                        val processed = AtomicInteger(0)
                        _progress.value = 0

                        allFolders.chunked(CHUNK_SIZE).forEach { batch ->
                            coroutineScope {
                                batch.map { folder ->
                                    async(context = Dispatchers.IO) {
                                        try {
                                            mangaDirectoryOps.refreshMangaChapters(mangaId = folder.id, baseUri = baseUri).onLeft {
                                                // TODO: Tratar melhor
                                                println("Error scanning chapters for ${folder.name}: $it")
                                            }
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
                    }.mapLeft { exception ->
                        when (exception) {
                            is IOException -> LibrarySyncError.DiskIOFailure(path = baseUri.toString(), exception)
                            is PatternSyntaxException -> LibrarySyncError.MalformedLibrary(cause = exception)
                            is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                            is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                            else -> LibrarySyncError.UnexpectedError(cause = exception)
                        }
                    }
                }
            }
        } finally {
            _isIndexing.value = false
        }
    }

    override fun observeLibrary(): StateFlow<List<MangaDirectoryDto>> {
        return directoryDao.getAllMangaDirectory().map { folders ->
            coroutineScope {
                folders.map { folder ->
                    async(context = Dispatchers.IO) {
                        folder.toDto()
                    }
                }.awaitAll()
            }
        }.stateIn(
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    }

    private suspend fun processFolderList(
        foldersToProcess: List<MangaDirectory>, 
        existingFolders: List<MangaDirectory>,
        baseUri: android.net.Uri? = null
    ) {
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
                            upsertFolder(folder, existingFolders, baseUri)
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
                            upsertFolder(folder, existingFolders, baseUri)
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

    private suspend fun upsertFolder(
        folder: MangaDirectory, 
        existingFolders: List<MangaDirectory>,
        baseUri: android.net.Uri? = null
    ) {
        val normalizedName = normalizeName(folder.name)
        val existing = existingFolders.find { normalizeName(it.name) == normalizedName }

        val finalMangaId = if (existing != null) {
            val updated = folder.copy(id = existing.id)
            directoryDao.update(entity = updated)
            existing.id
        } else {
            directoryDao.insert(entity = folder)
        }

        // Trigger chapter scan for new/updated folders
        mangaDirectoryOps.refreshMangaChapters(mangaId = finalMangaId, baseUri = baseUri)
    }

    private fun buildLibrary(context: Context, rootUri: Uri): List<MangaDirectory> {
        val pickedDir = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()
        
        // Otimização: Listagem via ContentResolver.query (40x mais rápido)
        val allChildren = ContentQueryHelper.listFiles(context, rootUri)
        val folders = allChildren.filter { it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR }

        return folders.map { folderMetadata ->
            val folderUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, folderMetadata.id)
            val folderDoc = DocumentFile.fromSingleUri(context, folderUri) ?: return@map null
            
            // Reusamos o ContentQueryHelper para listar itens dentro da pasta do mangá
            val folderChildren = ContentQueryHelper.listFiles(context, rootUri, folderMetadata.id)
            
            val bannerMetadata = folderChildren.firstOrNull { isBanner(it.name) }
            val coverMetadata = folderChildren.firstOrNull { isCover(it.name) }
            val hasComicInfo = folderChildren.any { it.name == "ComicInfo.xml" }

            val firstChapterName = folderChildren.firstOrNull { 
                FileExtension.isSupported(ext = it.name)
            }?.name

            val detectedTemplate = firstChapterName?.let {
                detectTemplate(fileName = it)
            }

            MangaDirectory(
                name = folderMetadata.name,
                path = folderUri.toString(),
                cover = coverMetadata?.let { DocumentsContract.buildDocumentUriUsingTree(rootUri, it.id).toString() },
                banner = bannerMetadata?.let { DocumentsContract.buildDocumentUriUsingTree(rootUri, it.id).toString() },
                chapterTemplate = detectedTemplate,
                lastModified = folderMetadata.lastModified,
                hasComicInfo = hasComicInfo,
            )
        }.filterNotNull()
    }

    private fun isCover(name: String?): Boolean {
        if (name == null) return false
        val lower = name.lowercase()
        // Prioriza arquivos com 'cover' no nome
        if (lower.contains("cover") && isImage(lower)) return true
        // Fallback: Qualquer imagem que pareça ser a capa (ex: folder.jpg, 00.jpg, front.png)
        return (lower.startsWith("folder") || lower.startsWith("front") || lower.startsWith("00")) && isImage(lower)
    }

    private fun isBanner(name: String?): Boolean {
        if (name == null) return false
        val lower = name.lowercase()
        return lower.contains("banner") && isImage(lower)
    }

    private fun isImage(name: String): Boolean {
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")
    }

    private fun normalizeName(name: String): String {
        return name.filter { it.isLetterOrDigit() }.lowercase()
    }
}