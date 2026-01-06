package br.acerola.manga.repository.adapter.local.sync

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import arrow.core.flatMap
import br.acerola.manga.config.preference.FileExtension
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.entity.archive.MangaDirectory
import br.acerola.manga.local.mapper.toMangaDirectoryModel
import br.acerola.manga.repository.port.DirectoryFsOps
import br.acerola.manga.repository.port.LibraryRepository
import br.acerola.manga.util.detectTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectorySyncRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val directoryDao: MangaDirectoryDao,
) : LibraryRepository<MangaDirectoryDto> {
    /**
     * Qualifier para saber que é:
     *
     * [br.acerola.manga.repository.adapter.local.manga.MangaDirectoryRepository]
     */
    @Inject
    @DirectoryFsOps
    lateinit  var mangaDirectoryOps: LibraryRepository.MangaOperations<MangaDirectoryDto>

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    companion object {
        const val CHUNK_SIZE = 50
        const val PROGRESS_THRESHOLD = 5
    }

    /**
     * Sincroniza a biblioteca local de mangás com o diretório selecionado no DataStore.
     *
     * Executa uma varredura completa nas pastas a partir de [baseUri], identificando novas adições,
     * atualizações de metadados e remoções. O processo é executado em **Dispatchers.IO**, garantindo
     * que a operação de I/O seja não bloqueante.
     *
     * **Fluxo operacional:**
     * 1. Constrói a lista atual de pastas usando o [ArchiveBuilder].
     * 2. Recupera as pastas já persistidas no banco via [directoryDao].
     * 3. Determina diferenças (novas, modificadas, removidas) com base em:
     *    - Timestamp (`lastModified`)
     *    - Mudanças de `cover` ou `banner`
     * 4. Remove entradas obsoletas e processa as novas/atualizadas.
     *
     * Quando não há nenhuma pasta detectada (nova ou existente), `_progress` é setado para `-1`,
     * sinalizando ociosidade.
     *
     * @param baseUri URI raiz que representa a origem do acervo de mangás.
     *
     * @see ArchiveBuilder.buildLibrary
     * @see processFolderList
     * @see directoryDao
     *
     * @throws java.io.IOException Se ocorrer falha no acesso ao diretório ou leitura de metadados.
     * @throws kotlinx.coroutines.CancellationException Se a coroutine for cancelada durante a sincronização.
     */
    override suspend fun syncMangas(baseUri: Uri?): Either<LibrarySyncError, Unit> {
        _isIndexing.value = true
        try {
            return withContext(context = Dispatchers.IO) {
                Either.catch {
                    if (baseUri === null) {
                        return@catch
                    }

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
                            // NOTE: Ele deleta os capitulos de forma recursiva, joga pro sqlite
                            directoryDao.delete(entity = folder)
                        }
                    }

                    processFolderList(foldersToProcess, existingFolders = databaseFolders)
                }.mapLeft { exception ->
                    when (exception) {
                        is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                        is IOException -> LibrarySyncError.DiskIOFailure(
                            path = baseUri?.toString() ?: "Unknown",
                            cause = exception
                        )

                        is SQLiteException -> LibrarySyncError.DatabaseError(
                            cause = exception
                        )

                        else -> LibrarySyncError.UnexpectedError(cause = exception)
                    }
                }
            }
        } finally {
            _isIndexing.value = false
        }
    }

    /**
     * Reindexa completamente o diretório de mangás, ignorando filtros diferenciais.
     *
     * Essa rotina força uma leitura total do acervo, sobrescrevendo dados anteriores, útil em casos de
     * corrupção de cache, inconsistência de metadados ou atualização massiva de arquivos.
     *
     * @param baseUri URI raiz do acervo de mangás a ser reprocessado.
     *
     * @see processFolderList
     * @see syncMangas
     */
    override suspend fun rescanMangas(baseUri: Uri?): Either<LibrarySyncError, Unit> {
        _isIndexing.value = true
        try {
            return withContext(context = Dispatchers.IO) {
                Either.catch {
                    // TODO: Tratar erro melhor
                    if (baseUri === null) {
                        return@catch
                    }

                    val foldersToProcess: List<MangaDirectory> =
                        buildLibrary(context, rootUri = baseUri)
                    if (foldersToProcess.isEmpty()) {
                        _progress.value = -1
                        return@catch
                    }

                    val existingFolders =
                        directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()
                    processFolderList(foldersToProcess, existingFolders)
                }.mapLeft { exception ->
                    when (exception) {
                        is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                        is IOException -> LibrarySyncError.DiskIOFailure(
                            path = baseUri?.toString() ?: "Unknown",
                            cause = exception
                        )

                        is SQLiteException -> LibrarySyncError.DatabaseError(
                            cause = exception
                        )

                        else -> LibrarySyncError.UnexpectedError(cause = exception)
                    }
                }
            }
        } finally {
            _isIndexing.value = false
        }
    }

    /**
     * Realiza uma varredura completa de toda a biblioteca, incluindo a atualização de capítulos.
     *
     * Combina as etapas de [rescanMangas] e [rescanChaptersByManga] de forma massiva e paralelizada,
     * utilizando *chunks* para otimizar o consumo de memória e controlar o progresso.
     *
     * O progresso é emitido dinamicamente via [progress], sendo finalizado em `-1` após a conclusão.
     *
     * @param baseUri URI raiz da biblioteca a ser completamente reindexada.
     *
     * @throws kotlinx.coroutines.CancellationException Se a operação for interrompida.
     */
    override suspend fun deepRescanLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> {
        _isIndexing.value = true
        try {
            return withContext(context = Dispatchers.IO) {
                rescanMangas(baseUri).flatMap {
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
                                            mangaDirectoryOps.rescanChaptersByManga(mangaId = folder.id)
                                                .onLeft {
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
                            is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                            is IOException -> LibrarySyncError.DiskIOFailure(
                                path = baseUri?.toString() ?: "Unknown",
                                cause = exception
                            )

                            is SQLiteException -> LibrarySyncError.DatabaseError(
                                cause = exception
                            )

                            else -> LibrarySyncError.UnexpectedError(cause = exception)
                        }
                    }
                }
            }
        } finally {
            _isIndexing.value = false
        }
    }

    /**
     * Processa e sincroniza em lote uma lista de pastas de mangás.
     *
     * A função gerencia chunks para evitar sobrecarga de memória e atualiza o estado de progresso
     * em intervalos configuráveis. Também chama [upsertFolder] para cada pasta detectada.
     *
     * @param foldersToProcess Lista de pastas a serem sincronizadas.
     * @param existingFolders Lista de pastas já persistidas no banco.
     *
     * @see upsertFolder
     * @see directoryDao
     */
    private suspend fun processFolderList(foldersToProcess: List<MangaDirectory>, existingFolders: List<MangaDirectory>) {
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

    /**
     * Insere ou atualiza uma pasta de mangá no banco, conforme sua existência prévia.
     *
     * Caso a pasta já exista, seus metadados são atualizados mantendo o mesmo `id`.
     * Se for uma nova entrada, é criada uma nova linha no banco.
     *
     * @param folder Entidade [MangaDirectory] a ser inserida ou atualizada.
     * @param existingFolders Lista completa de pastas persistidas para verificação de duplicidade.
     */
    private suspend fun upsertFolder(folder: MangaDirectory, existingFolders: List<MangaDirectory>) {
        val normalizedName = normalizeName(folder.name)
        val existing = existingFolders.find { normalizeName(it.name) == normalizedName }

        if (existing != null) {
            directoryDao.update(entity = folder.copy(id = existing.id))
            return
        }

        directoryDao.insert(entity = folder)
    }

    private fun buildLibrary(context: Context, rootUri: Uri): List<MangaDirectory> {
        val pickedDir = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()

        return pickedDir.listFiles().filter { it.isDirectory }.map { folder ->
            val banner = folder.listFiles().firstOrNull { isBanner(file = it) }
            val cover = folder.listFiles().firstOrNull { isCover(file = it) }

            val firstChapter = folder.listFiles().firstOrNull { file ->
                file.isFile && FileExtension.isSupported(ext = file.name)
            }

            val detectedTemplate = firstChapter?.name?.let {
                detectTemplate(fileName = it)
            }

            folder.toMangaDirectoryModel(cover, banner, detectedTemplate)
        }
    }

    // TODO: Fazer "cover" ".jpg" ".png" ser um dicionario desse object, uma constante a ser usada.
    private fun isCover(file: DocumentFile): Boolean {
        val name = file.name?.lowercase() ?: return false
        return name.contains(other = "cover") && (name.endsWith(suffix = ".jpg") || name.endsWith(suffix = ".png"))
    }

    private fun isBanner(file: DocumentFile): Boolean {
        val name = file.name?.lowercase() ?: return false
        return name.contains(other = "banner") && (name.endsWith(suffix = ".jpg") || name.endsWith(suffix = ".png"))
    }

    private fun normalizeName(name: String): String {
        return name.filter { it.isLetterOrDigit() }.lowercase()
    }

}