package br.acerola.manga.repository.adapter.local.manga

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.Either
import br.acerola.manga.config.preference.MangaDirectoryPreference
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.exception.IntegrityException
import br.acerola.manga.error.exception.MangadexRequestException
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.author.AuthorDao
import br.acerola.manga.local.database.dao.metadata.genre.GenreDao
import br.acerola.manga.local.database.entity.archive.MangaDirectory
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.local.mapper.toModel
import br.acerola.manga.repository.di.Mangadex
import br.acerola.manga.repository.port.MangaManagementRepository
import br.acerola.manga.repository.port.RemoteInfoOperationsRepository
import br.acerola.manga.service.archive.MangaSaveCoverService
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class MangadexMangaRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val genreDao: GenreDao,
    private val authorDao: AuthorDao,
    private val directoryDao: MangaDirectoryDao,
    private val coverService: MangaSaveCoverService,
    private val mangaRemoteInfoDao: MangaRemoteInfoDao,
) : MangaManagementRepository<MangaRemoteInfoDto> {

    @Inject
    @Mangadex
    lateinit var mangadexChapterInfoService: RemoteInfoOperationsRepository<ChapterRemoteInfoDto, String>

    @Inject
    @Mangadex
    lateinit var mangadexMangaInfoService: RemoteInfoOperationsRepository<MangaRemoteInfoDto, String>

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    /**
     * Atualiza os metadados de um mangá específico já existente no banco.
     */
    override suspend fun refreshManga(mangaId: Long): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            _isIndexing.value = true
            try {
                Either.catch {
                    val remoteInfo = mangaRemoteInfoDao.getMangaById(mangaId).firstOrNull() ?: return@catch
                    val directory = directoryDao.getMangaDirectoryByName(mangaName = remoteInfo.title) ?: return@catch

                    executeSync(folders = listOf(directory), baseUri = null)
                }.mapLeft { exception ->
                    when (exception) {
                        is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                        else -> LibrarySyncError.UnexpectedError(cause = exception)
                    }
                }
            } finally {
                _isIndexing.value = false
            }
        }

    /**
     * Busca metadados remotos para pastas locais que ainda não possuem associação.
     */
    override suspend fun incrementalScan(baseUri: Uri?): Either<LibrarySyncError, Unit> {
        _isIndexing.value = true

        try {
            return withContext(context = Dispatchers.IO) {
                Either.catch {
                    val localDirectories = directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()
                    val allRemoteMangaInfo = mangaRemoteInfoDao.getAllMangaRemoteInfo().firstOrNull() ?: emptyList()
                    val existingTitles = allRemoteMangaInfo.map { normalizeName(name = it.title) }.toSet()

                    val remoteInfoToSync = localDirectories.filter {
                        normalizeName(it.name) !in existingTitles
                    }

                    if (remoteInfoToSync.isEmpty()) {
                        _progress.value = -1
                        return@catch
                    }

                    executeSync(folders = remoteInfoToSync, baseUri)
                }.mapLeft { exception ->
                    when (exception) {
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
                    val mangaLibraryFolders = directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()

                    if (mangaLibraryFolders.isEmpty()) {
                        _progress.value = -1
                        return@catch
                    }

                    executeSync(folders = mangaLibraryFolders, baseUri)
                }.mapLeft { exception ->
                    when (exception) {
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
        return refreshLibrary(baseUri)
    }

    override fun observeLibrary(): StateFlow<List<MangaRemoteInfoDto>> {
        return mangaRemoteInfoDao.getAllMangasWithRelations().map { remoteInfoRelations ->
            coroutineScope {
                remoteInfoRelations.map { remoteInfo ->
                    async(context = Dispatchers.IO) {
                        remoteInfo.toDto()
                    }
                }.awaitAll()
            }
        }.stateIn(
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    }

    private suspend fun executeSync(folders: List<MangaDirectory>, baseUri: Uri?) {
        val total = folders.size
        _progress.value = 0

        val rootPath = baseUri?.toString() ?: MangaDirectoryPreference.folderUriFlow(context).firstOrNull()
        if (rootPath.isNullOrBlank()) {
            _progress.value = -1
            return
        }

        val rootUri = rootPath.toUri()
        folders.forEachIndexed { index, current ->
            val result = Either.catch {
                val title = current.name
                val folderNameNormalized = normalizeName(name = title)

                val fetchedListResult = mangadexMangaInfoService.searchInfo(manga = title)
                val fetchedList = fetchedListResult.getOrNull() ?: emptyList()

                val bestMatch = fetchedList.find { candidate ->
                    normalizeName(name = candidate.title) == folderNameNormalized || normalizeName(name = candidate.romanji.orEmpty()) == folderNameNormalized
                } ?: fetchedList.firstOrNull()

                if (bestMatch != null) {
                    val mangaId = mangaRemoteInfoDao.insert(
                        entity = bestMatch.toModel()
                    )

                    if (mangaId != -1L) {
                        bestMatch.authors?.let {
                            authorDao.insert(entity = it.toModel(mangaId = mangaId))
                        }

                        bestMatch.genre.forEach {
                            genreDao.insert(entity = it.toModel(mangaId = mangaId))
                        }

                        bestMatch.cover?.let { dto ->
                            coverService.processCover(
                                coverDto = dto,
                                rootUri = rootUri,
                                folderId = current.id,
                                mangaFolderName = current.name,
                                mangaRemoteInfoFk = mangaId
                            )
                        }
                    }
                }
            }

            result.onLeft { exception ->
                when (exception) {
                    is SQLiteException, is IntegrityException -> LibrarySyncError.DatabaseError(cause = exception)
                    is IOException -> LibrarySyncError.DiskIOFailure(path = current.path, cause = exception)
                    is MangadexRequestException -> LibrarySyncError.MangadexError(cause = exception)
                    else -> LibrarySyncError.UnexpectedError(cause = exception)
                }
            }

            val currentProgress = (((index + 1).toFloat() / total.toFloat()) * 100).roundToInt()
            _progress.value = currentProgress
        }

        _progress.value = -1
    }

    private fun normalizeName(name: String): String {
        return name.filter { it.isLetterOrDigit() }.lowercase()
    }
}
