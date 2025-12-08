package br.acerola.manga.domain.service.library.sync

import android.content.Context
import android.net.Uri
import br.acerola.manga.domain.builder.ArchiveBuilder
import br.acerola.manga.domain.database.dao.database.archive.ChapterFileDao
import br.acerola.manga.domain.database.dao.database.archive.MangaFolderDao
import br.acerola.manga.domain.model.archive.MangaFolder
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.domain.service.library.manga.FolderMangaOperation
import br.acerola.manga.shared.dto.archive.MangaFolderDto
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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.chunked
import kotlin.collections.map

class ArchiveSyncService(
    private val context: Context,
    private val folderDao: MangaFolderDao,
    private val chapterDao: ChapterFileDao,
    private val mangaOps: LibraryPort.MangaOperations<MangaFolderDto> = FolderMangaOperation(
        context,
        folderDao,
        chapterDao
    ),
) : LibraryPort<MangaFolderDto> {
    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

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
     * 2. Recupera as pastas já persistidas no banco via [folderDao].
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
     * @see folderDao
     *
     * @throws java.io.IOException Se ocorrer falha no acesso ao diretório ou leitura de metadados.
     * @throws kotlinx.coroutines.CancellationException Se a coroutine for cancelada durante a sincronização.
     */
    override suspend fun syncMangas(baseUri: Uri?) = withContext(context = Dispatchers.IO) {
        // TODO: Tratar erro melhor
        if (baseUri === null) {
            return@withContext
        }

        val folders: List<MangaFolder> = ArchiveBuilder.buildLibrary(context, rootUri = baseUri)
        val existingFolders: List<MangaFolder> = folderDao.getAllMangasFolders().firstOrNull() ?: emptyList()

        if (folders.isEmpty() && existingFolders.isEmpty()) {
            _progress.value = -1
            return@withContext
        }

        val existingFoldersMap = existingFolders.associateBy { normalizeName(it.name) }
        val foldersMap = folders.associateBy { normalizeName(it.name) }

        val foldersToProcess = folders.filter { folder ->
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

        val removedFolders = existingFolders.filter { normalizeName(it.name) !in foldersMap }

        if (removedFolders.isNotEmpty()) {
            removedFolders.forEach { folder ->
                // NOTE: Ele deleta os capitulos de forma recursiva, joga pro sqlite
                folderDao.delete(entity = folder)
            }
        }

        processFolderList(foldersToProcess, existingFolders)
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
    override suspend fun rescanMangas(baseUri: Uri?) = withContext(context = Dispatchers.IO) {
        // TODO: Tratar erro melhor
        if (baseUri === null) {
            return@withContext
        }

        val foldersToProcess: List<MangaFolder> = ArchiveBuilder.buildLibrary(context, rootUri = baseUri)
        if (foldersToProcess.isEmpty()) {
            _progress.value = -1
            return@withContext
        }

        val existingFolders = folderDao.getAllMangasFolders().firstOrNull() ?: emptyList()
        processFolderList(foldersToProcess, existingFolders)
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
    override suspend fun deepRescanLibrary(baseUri: Uri?) = withContext(context = Dispatchers.IO) {
        rescanMangas(baseUri)
        val allFolders = folderDao.getAllMangasFolders().firstOrNull() ?: emptyList()

        if (allFolders.isEmpty()) {
            _progress.value = -1
            return@withContext
        }

        val total = allFolders.size
        val processed = AtomicInteger(0)
        _progress.value = 0

        allFolders.chunked(CHUNK_SIZE).forEach { batch ->
            coroutineScope {
                batch.map { folder ->
                    async(context = Dispatchers.IO) {
                        try {
                            mangaOps.rescanChaptersByManga(mangaId = folder.id)
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
     * Processa e sincroniza em lote uma lista de pastas de mangás.
     *
     * A função gerencia chunks para evitar sobrecarga de memória e atualiza o estado de progresso
     * em intervalos configuráveis. Também chama [upsertFolder] para cada pasta detectada.
     *
     * @param foldersToProcess Lista de pastas a serem sincronizadas.
     * @param existingFolders Lista de pastas já persistidas no banco.
     *
     * @see upsertFolder
     * @see folderDao
     */
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

    /**
     * Insere ou atualiza uma pasta de mangá no banco, conforme sua existência prévia.
     *
     * Caso a pasta já exista, seus metadados são atualizados mantendo o mesmo `id`.
     * Se for uma nova entrada, é criada uma nova linha no banco.
     *
     * @param folder Entidade [MangaFolder] a ser inserida ou atualizada.
     * @param existingFolders Lista completa de pastas persistidas para verificação de duplicidade.
     */
    private suspend fun upsertFolder(folder: MangaFolder, existingFolders: List<MangaFolder>) {
        val normalizedName = normalizeName(folder.name)
        val existing = existingFolders.find { normalizeName(it.name) == normalizedName }

        if (existing != null) {
            folderDao.update(entity = folder.copy(id = existing.id))
            return
        }

        folderDao.insert(entity = folder)
    }

    private fun normalizeName(name: String): String {
        return name.filter { it.isLetterOrDigit() }.lowercase()
    }
}