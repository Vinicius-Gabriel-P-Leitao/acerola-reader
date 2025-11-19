package br.acerola.manga.domain.service.library.manga

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.domain.database.dao.archive.ChapterFileDao
import br.acerola.manga.domain.database.dao.archive.MangaFolderDao
import br.acerola.manga.domain.mapper.toDto
import br.acerola.manga.domain.model.archive.ChapterFile
import br.acerola.manga.domain.model.archive.MangaFolder
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.shared.config.FileExtension
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class MangaFolderService(
    private val context: Context,
    private val folderDao: MangaFolderDao,
    private val chapterDao: ChapterFileDao
) : LibraryPort.MangaOperations {
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
