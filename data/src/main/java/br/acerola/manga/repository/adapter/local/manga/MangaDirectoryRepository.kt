package br.acerola.manga.repository.adapter.local.manga

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import arrow.core.Either
import br.acerola.manga.config.preference.FileExtension
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.database.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.mapper.toChapterArchiveModel
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.repository.port.LibraryRepository
import br.acerola.manga.util.sha256
import br.acerola.manga.util.templateToRegex
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaDirectoryRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val directoryDao: MangaDirectoryDao,
    private val archiveDao: ChapterArchiveDao
) : LibraryRepository.MangaOperations<MangaDirectoryDto> {
    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    /**
     * Reescaneia todos os capítulos vinculados a um mangá específico.
     *
     * Remove registros antigos e reindexa todos os arquivos de capítulo encontrados no diretório da pasta
     * correspondente. A operação é segura e idempotente, garantindo consistência entre disco e banco.
     *
     * @param mangaId Identificador da pasta de mangá alvo.
     */
    override suspend fun rescanChaptersByManga(mangaId: Long): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            _isIndexing.value = true
            _progress.value = 0

            Either.catch {
                val folder = directoryDao.getMangaDirectoryById(mangaId = mangaId) ?: return@catch
                val folderDoc = DocumentFile.fromTreeUri(context, folder.path.toUri()) ?: return@catch

                // NOTE: Isso aqui vai existir só quando eu quiser pegar os que não existem no DB
                val chaptersExist = archiveDao.countChaptersByMangaDirectory(folderId = mangaId) > 0

                if (chaptersExist && folder.lastModified >= folderDoc.lastModified()) {
                    return@catch
                }

                _progress.value = 10

                val chapterFiles = folderDoc.listFiles().filter { it.isFile }.filter { file ->
                    FileExtension.isSupported(ext = file.name)
                }

                _progress.value = 30

                archiveDao.deleteChaptersByMangaDirectoryId(folderId = mangaId)

                val chapterRegex = templateToRegex(template = folder.chapterTemplate ?: "{value}{sub}.*.cbz")

                val chapters = chapterFiles.mapIndexedNotNull { index, file ->
                    val name = file.name ?: return@mapIndexedNotNull null
                    val match = chapterRegex.matchEntire(input = name) ?: return@mapIndexedNotNull null

                    val integerPart = match.groupValues[1].toInt()

                    val fractionalPartRaw = match.groupValues.getOrNull(index = 2)
                    val fractionalPart = fractionalPartRaw?.toIntOrNull() ?: 0

                    val chapterSort = if (fractionalPart == 0) integerPart.toString()
                    else "$integerPart.$fractionalPart"

                    val currentProgress = 30 + ((index + 1) * 60 / chapterFiles.size)
                    _progress.value = currentProgress

                    file.toChapterArchiveModel(
                        mangaId = mangaId,
                        chapterSort = chapterSort,
                        checksum = file.sha256(context)
                    )
                }

                if (chapters.isNotEmpty()) {
                    archiveDao.insertAll(*chapters.toTypedArray())
                }

                if (folder.lastModified < folderDoc.lastModified()) {
                    directoryDao.update(entity = folder.copy(lastModified = folderDoc.lastModified()))
                }

                _progress.value = 100
            }.mapLeft { exception ->
                when (exception) {
                    is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                    is IOException -> LibrarySyncError.DiskIOFailure(path = "Unknown", cause = exception)

                    is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                    else -> LibrarySyncError.UnexpectedError(cause = exception)
                }
            }.also {
                _isIndexing.value = false
                _progress.value = -1
            }
        }

    /**
     * Retorna um fluxo reativo contendo todos os mangás e seus capítulos associados, os dados são inseridos no sync.
     *
     * Combina as emissões de [MangaDirectoryDao] e [ChapterArchiveDao], convertendo o resultado para
     * objetos [MangaDirectoryDto] via [toDto].
     *
     * O fluxo é *stateful* e inicializa de forma preguiçosa (`SharingStarted.Lazily`).
     *
     * @return [StateFlow] contendo a lista de [MangaDirectoryDto] atualizada em tempo real.
     */
    override fun loadMangas(): StateFlow<List<MangaDirectoryDto>> {
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
}
