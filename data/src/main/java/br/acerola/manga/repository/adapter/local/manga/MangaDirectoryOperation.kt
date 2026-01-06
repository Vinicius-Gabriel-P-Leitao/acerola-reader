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
import br.acerola.manga.local.database.entity.archive.ChapterArchive
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaDirectoryOperation @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val directoryDao: MangaDirectoryDao,
    private val archiveDao: ChapterArchiveDao
) : LibraryRepository.MangaOperations<MangaDirectoryDto> {
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
            Either.catch {
                val folder = directoryDao.getMangaDirectoryById(mangaId = mangaId) ?: return@catch
                val folderDoc = DocumentFile.fromTreeUri(context, folder.path.toUri()) ?: return@catch

//                NOTE: Isso aqui vai existir só quando eu quiser pegar os que não existem no DB
//                val chaptersExist = archiveDao.countChaptersByMangaDirectory(folderId = mangaId) > 0
//
//                if (chaptersExist && folder.lastModified >= folderDoc.lastModified()) {
//                    return@catch
//                }

                val chapterFiles = folderDoc.listFiles().filter { it.isFile }.filter { file ->
                    FileExtension.isSupported(ext = file.name)
                }

                archiveDao.deleteChaptersByMangaDirectoryId(folderId = mangaId)

                // TODO: Fazer lógica de validação melhor
                val chapterRegex = templateToRegex(template = folder.chapterTemplate ?: "{value}{sub}.*.cbz")
                println("DEBUG: chapterRegex=$chapterRegex")

                // TODO: Tratar erro de quando não consegue dar nenhum match, lembrar de avisar o miserável de que o mangá
                //  tem que seguir um formato só, mais de um a lista fica desorganizada.
                val chapters = chapterFiles.mapNotNull { file ->
                    val name = file.name ?: return@mapNotNull null
                    val match = chapterRegex.matchEntire(input = name) ?: return@mapNotNull null

                    val mainInt = match.groupValues[1].toInt()

                    val subRaw = match.groupValues.getOrNull(index = 2)
                    val subInt = subRaw?.toIntOrNull() ?: 0

                    val chapterSort = if (subInt == 0) mainInt.toString()
                    else "$mainInt.$subInt"

                    ChapterArchive(
                        chapter = name,
                        path = file.uri.toString(),
                        checksum = file.sha256(context),
                        chapterSort = chapterSort,
                        folderPathFk = mangaId
                    )
                }

                if (chapters.isNotEmpty()) {
                    archiveDao.insertAll(*chapters.toTypedArray())
                }

                if (folder.lastModified < folderDoc.lastModified()) {
                    directoryDao.update(entity = folder.copy(lastModified = folderDoc.lastModified()))
                }
            }.mapLeft { exception ->
                when (exception) {
                    is SecurityException -> LibrarySyncError.FolderAccessDenied(cause = exception)
                    is IOException -> LibrarySyncError.DiskIOFailure(path = "Unknown", cause = exception)

                    is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                    else -> LibrarySyncError.UnexpectedError(cause = exception)
                }
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
