package br.acerola.manga.repository.adapter.local.manga

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import br.acerola.manga.config.preference.FileExtension
import br.acerola.manga.dto.archive.ChapterArchivePageDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
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
     *
     * @throws java.io.FileNotFoundException Se a pasta associada ao mangá não for encontrada.
     * @throws SecurityException Se o aplicativo perder a permissão de acesso ao [Uri].
     */
    override suspend fun rescanChaptersByManga(mangaId: Long) = withContext(context = Dispatchers.IO) {
        val folder = directoryDao.getMangaDirectoryById(mangaId = mangaId) ?: return@withContext
        val folderDoc = DocumentFile.fromTreeUri(context, folder.path.toUri()) ?: return@withContext

        val chaptersExist = archiveDao.countChaptersByMangaDirectory(folderId = mangaId) > 0

        if (chaptersExist && folder.lastModified >= folderDoc.lastModified()) {
            return@withContext
        }

        val chapterFiles = folderDoc.listFiles().filter { it.isFile }.filter { file ->
            FileExtension.isSupported(ext = file.name)
        }

        archiveDao.deleteChaptersByMangaDirectoryId(folderId = mangaId)

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

            // TODO: Tranformar em um toModel
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
                        val firstPage: ChapterArchivePageDto = loadFirstPage(folderId = folder.id)
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
     * Monta o DTO de cada mangá, necessário por que os mangás tem capitulos paginados.
     *
     * @param folderId Identificador da pasta de mangá.
     */
    private suspend fun loadFirstPage(folderId: Long): ChapterArchivePageDto {
        // TODO: Fazer isso vim de config global, procurar mais locais onde isso ocorre, talvez mudar assinatura do
        //  método pai e receber via props
        val pageSize = 20
        val total = archiveDao.countChaptersByMangaDirectory(folderId)
        val initial = archiveDao.getChaptersPaged(folderId, pageSize, offset = 0).firstOrNull() ?: emptyList()

        return ChapterArchivePageDto(
            items = initial.map { it.toDto() },
            pageSize = pageSize,
            total = total,
            page = 0,
        )
    }
}
