package br.acerola.manga.repository.adapter.local.sync

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import br.acerola.manga.config.preference.MangaDirectoryPreference
import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.author.AuthorDao
import br.acerola.manga.local.database.dao.metadata.genre.GenreDao
import br.acerola.manga.local.database.entity.archive.MangaDirectory
import br.acerola.manga.local.database.entity.metadata.relationship.Author
import br.acerola.manga.local.database.entity.metadata.relationship.Genre
import br.acerola.manga.local.database.entity.metadata.relationship.TypeAuthor
import br.acerola.manga.local.mapper.toModel
import br.acerola.manga.repository.port.ApiRepository
import br.acerola.manga.repository.port.LibraryRepository
import br.acerola.manga.repository.port.Mangadex
import br.acerola.manga.service.archive.MangaSaveCoverService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class MangadexSyncService @Inject constructor(
    private val authorDao: AuthorDao,
    private val genreDao: GenreDao,
    private val directoryDao: MangaDirectoryDao,
    private val remoteInfoDao: MangaRemoteInfoDao,
    private val coverService: MangaSaveCoverService,
    @param:ApplicationContext private val context: Context,
) : LibraryRepository<MangaRemoteInfoDto> {
    /**
     * Qualifier para saber que é:
     *
     * [br.acerola.manga.repository.adapter.remote.mangadex.manga.MangadexMangaInfoService]
     */
    @Inject
    @Mangadex
    lateinit var mangadexMangaInfoService: ApiRepository.RemoteInfoOperations<MangaRemoteInfoDto, String>

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun syncMangas(baseUri: Uri?) {
        _isIndexing.value = true

        try {
            withContext(context = Dispatchers.IO) {
                val localDirectories = directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()
                val allRemoteMangaInfo = remoteInfoDao.getAllMangaRemoteInfo().firstOrNull() ?: emptyList()

                val existingTitles = allRemoteMangaInfo.map { normalizeName(it.title) }.toSet()

                val remoteInfoToSync = localDirectories.filter {
                    normalizeName(it.name) !in existingTitles
                }

                if (remoteInfoToSync.isEmpty()) {
                    _progress.value = -1
                    return@withContext
                }

                executeSync(folders = remoteInfoToSync, baseUri)
            }
        } finally {
            _isIndexing.value = false
        }
    }

    override suspend fun rescanMangas(baseUri: Uri?) {
        _isIndexing.value = true
        try {
            withContext(context = Dispatchers.IO) {
                val mangaLibraryFolders = directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()

                if (mangaLibraryFolders.isEmpty()) {
                    _progress.value = -1
                    return@withContext
                }

                executeSync(folders = mangaLibraryFolders, baseUri)
            }
        } finally {
            _isIndexing.value = false
        }
    }

    // NOTE: Pensar em forma de fazer isso futuramente, mas até agora é função inutil
    override suspend fun deepRescanLibrary(baseUri: Uri?) {
        rescanMangas(baseUri)
    }

    private suspend fun executeSync(folders: List<MangaDirectory>, baseUri: Uri?) {
        val total = folders.size
        _progress.value = 0

        // NOTE: Se a URL não for passada ele usa a de preferencia.
        val rootPath = baseUri?.toString() ?: MangaDirectoryPreference.folderUriFlow(context).firstOrNull()
        if (rootPath.isNullOrBlank()) {
            _progress.value = -1
            return
        }

        val rootUri = rootPath.toUri()

        folders.forEachIndexed { index, current ->
            try {
                val title = current.name
                val fetchedList: List<MangaRemoteInfoDto> = mangadexMangaInfoService.searchInfo(manga = title)
                val folderNameNormalized = normalizeName(name = title)

                val bestMatch: MangaRemoteInfoDto? = fetchedList.find { candidate ->
                    normalizeName(name = candidate.title) == folderNameNormalized || normalizeName(
                        name = candidate.romanji ?: ""
                    ) == folderNameNormalized
                } ?: fetchedList.firstOrNull()

                if (bestMatch != null) {
                    val authorId = bestMatch.authors?.let { saveAndGetAuthorId(dto = it) }
                    val genreId = bestMatch.genre.firstOrNull()?.let { saveAndGetGenreId(dto = it) }
                    val coverId = bestMatch.cover?.let { dto ->
                        coverService.processCover(
                            coverDto = dto,
                            rootUri = rootUri,
                            folderId = current.id,
                            mangaFolderName = current.name,
                        )
                    }

                    val newMangaEntity = bestMatch.toModel(
                        authorId = authorId,
                        coverId = coverId,
                        genreId = genreId
                    )

                    remoteInfoDao.insert(entity = newMangaEntity)
                }

            } catch (exception: Exception) {
                // TODO: Normalizar esse erro, ao invez de um log
                println("Erro ao sincronizar $exception")
                throw exception
            } finally {
                val currentProgress = (((index + 1).toFloat() / total.toFloat()) * 100).roundToInt()
                _progress.value = currentProgress
            }
        }

        _progress.value = -1
    }

    private suspend fun saveAndGetAuthorId(dto: AuthorDto): Long {
        val insertedId = authorDao.insert(
            entity = Author(
                type = TypeAuthor.getByType(dto.type),
                mirrorId = dto.id,
                name = dto.name,
            )
        )

        // TODO: Criar uma string e tratar erros melhor
        return if (insertedId != -1L) {
            insertedId
        } else {
            authorDao.getAuthorByMirrorId(mirrorId = dto.id)?.id
                ?: throw IllegalStateException("Autor deveria existir mas não foi encontrado: ${dto.name}")
        }
    }

    private suspend fun saveAndGetGenreId(dto: GenreDto): Long {
        val insertedId = genreDao.insert(
            entity = Genre(
                mirrorId = dto.id,
                genre = dto.name
            )
        )

        // TODO: Criar uma string e tratar erros melhor
        return if (insertedId != -1L) {
            insertedId
        } else {
            genreDao.getGenreByMirrorId(mirrorId = dto.id)?.id
                ?: throw IllegalStateException("Gênero deveria existir")
        }
    }

    private fun normalizeName(name: String): String {
        return name.filter { it.isLetterOrDigit() }.lowercase()
    }
}