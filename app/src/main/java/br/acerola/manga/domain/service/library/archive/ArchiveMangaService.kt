package br.acerola.manga.domain.service.library.archive

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
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.shared.config.FileExtension
import br.acerola.manga.shared.dto.archive.ChapterFileDto
import br.acerola.manga.shared.dto.archive.ChapterPageDto
import br.acerola.manga.shared.dto.archive.MangaFolderDto
import br.acerola.manga.shared.util.templateToRegex
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
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class ArchiveMangaService(
    private val context: Context,
    private val folderDao: MangaFolderDao,
    private val chapterDao: ChapterFileDao
) : LibraryPort {
    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val CHUNK_SIZE = 50
    private val PROGRESS_THRESHOLD = 5

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
    override suspend fun syncMangas(baseUri: Uri) = withContext(context = Dispatchers.IO) {
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
    override suspend fun rescanMangas(baseUri: Uri) = withContext(context = Dispatchers.IO) {
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
    override suspend fun deepRescanLibrary(baseUri: Uri) = withContext(context = Dispatchers.IO) {
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
                            rescanChaptersByManga(mangaId = folder.id)
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
     * Reescaneia todos os capítulos vinculados a um mangá específico.
     *
     * Remove registros antigos e reindexa todos os arquivos de capítulo encontrados no diretório da pasta
     * correspondente. A operação é segura e idempotente, garantindo consistência entre disco e banco.
     *
     * @param mangaId Identificador da pasta de mangá alvo.
     *
     * @throws java.io.FileNotFoundException Se a pasta associada ao mangá não for encontrada.
     * @throws SecurityException Se o aplicativo perder a permissão de acesso ao [Uri].
     */
    override suspend fun rescanChaptersByManga(mangaId: Long) = withContext(context = Dispatchers.IO) {
        val folder = folderDao.getMangaFolderById(mangaId = mangaId) ?: return@withContext
        val folderDoc = DocumentFile.fromTreeUri(context, folder.path.toUri()) ?: return@withContext

        val chaptersExist = chapterDao.countChaptersByFolder(folderId = mangaId) > 0

        if (chaptersExist && folder.lastModified >= folderDoc.lastModified()) {
            return@withContext
        }

        val chapterFiles = folderDoc.listFiles().filter { it.isFile }.filter { file ->
            FileExtension.isSupported(ext = file.name)
        }

        chapterDao.deleteChaptersByFolderId(folderId = mangaId)

        // TODO: Fazer lógica de validação melhor
        val chapterRegex = templateToRegex(template = folder.chapterTemplate ?: "{value}.cbz")

        // TODO: Tratar erro de quando não consegue dar nenhum match, lembrar de avisar o miserável de que o mangá
        //  tem que seguir um formato só, mais de um a lista fica desorganizada.
        val chapters = chapterFiles.mapNotNull { file ->
            val name = file.name ?: return@mapNotNull null

            val match = chapterRegex.matchEntire(input = name) ?: return@mapNotNull null
            val value = match.groups[1]?.value?.toDoubleOrNull() ?: return@mapNotNull null

            val subGroup = if (match.groups.size > 2) match.groups[2] else null
            val sub = subGroup?.value?.toDoubleOrNull() ?: 0.0

            val chapterSort = "%05.2f".format(value + sub)

            ChapterFile(
                chapter = name,
                path = file.uri.toString(),
                chapterSort = chapterSort,
                folderPathFk = mangaId
            )
        }

        if (chapters.isNotEmpty()) {
            chapterDao.insertAll(chapters)
        }

        if (folder.lastModified < folderDoc.lastModified()) {
            folderDao.updateMangaFolder(manga = folder.copy(lastModified = folderDoc.lastModified()))
        }
    }

    /**
     * Retorna um fluxo reativo contendo todos os mangás e seus capítulos associados.
     *
     * Combina as emissões de [MangaFolderDao] e [ChapterFileDao], convertendo o resultado para
     * objetos [MangaFolderDto] via [toDto].
     *
     * O fluxo é *stateful* e inicializa de forma preguiçosa (`SharingStarted.Lazily`).
     *
     * @return [StateFlow] contendo a lista de [MangaFolderDto] atualizada em tempo real.
     */
    override fun loadMangas(): StateFlow<List<MangaFolderDto>> {
        return folderDao.getAllMangasFolders().map { folders ->
            coroutineScope {
                folders.map { folder ->
                    async(context = Dispatchers.IO) {
                        val firstPage: ChapterPageDto = loadFirstPage(folderId = folder.id)
                        folder.toDto(firstPage)
                    }
                }.awaitAll()
            }
        }.stateIn(
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    }

    /**
     * Retorna um fluxo reativo contendo todos os capítulos pertencentes a um mangá específico.
     *
     * Cada entidade [ChapterFile] é convertida para [ChapterFileDto] por meio do mapeador [toDto].
     *
     * @param mangaId Identificador único do mangá.
     * @return [StateFlow] com a lista de capítulos atualizada dinamicamente.
     */
    override fun loadChapterByManga(mangaId: Long): StateFlow<List<ChapterFileDto>> {
        return chapterDao.getChaptersByFolder(folderId = mangaId).map { list ->
            list.map { it.toDto() }
        }.stateIn(
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    }

    // TODO: Documentar
    override suspend fun loadNextPage(folderId: Long, total: Int, page: Int, pageSize: Int): ChapterPageDto {
        val offset = page * pageSize
        val items = chapterDao.getChaptersPaged(folderId, pageSize, offset).firstOrNull()?.map {
            it.toDto()
        } ?: emptyList()

        return ChapterPageDto(
            items = items,
            pageSize = pageSize,
            page = page,
            total = total
        )
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
        val existing = existingFolders.find { it.path == folder.path }

        if (existing != null) {
            folderDao.updateMangaFolder(manga = folder.copy(id = existing.id))
            return
        }

        folderDao.insertMangaFolder(manga = folder)
    }

    // TODO: Documentar
    private suspend fun loadFirstPage(folderId: Long): ChapterPageDto {
        val pageSize = 20
        val total = chapterDao.countChaptersByFolder(folderId)
        val initial = chapterDao.getChaptersPaged(folderId, pageSize, offset = 0).firstOrNull() ?: emptyList()

        return ChapterPageDto(
            items = initial.map { it.toDto() }, pageSize = pageSize, page = 0, total = total
        )
    }
}
