package br.acerola.manga.repository.adapter.local.sync

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.core.net.toUri
import arrow.core.Either
import br.acerola.manga.config.preference.MangaDirectoryPreference
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.exception.IntegrityException
import br.acerola.manga.error.exception.MangadexRequestException
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.database.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.database.dao.metadata.MangaRemoteInfoDao
import br.acerola.manga.local.database.dao.metadata.author.AuthorDao
import br.acerola.manga.local.database.dao.metadata.genre.GenreDao
import br.acerola.manga.local.database.entity.archive.MangaDirectory
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
import java.io.IOException
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

    override suspend fun syncMangas(baseUri: Uri?): Either<LibrarySyncError, Unit> {
        _isIndexing.value = true

        try {
            return withContext(context = Dispatchers.IO) {
                Either.catch {
                    val localDirectories = directoryDao.getAllMangaDirectory().firstOrNull() ?: emptyList()
                    val allRemoteMangaInfo = remoteInfoDao.getAllMangaRemoteInfo().firstOrNull() ?: emptyList()
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

    override suspend fun rescanMangas(baseUri: Uri?): Either<LibrarySyncError, Unit> {
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

    // NOTE: Pensar em forma de fazer isso futuramente, mas até agora é função inutil
    override suspend fun deepRescanLibrary(baseUri: Uri?): Either<LibrarySyncError, Unit> {
        return rescanMangas(baseUri)
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

                    val authorId = bestMatch.authors?.let {
                        authorDao.insertOrGetId(entity = it.toModel())
                    }

                    val genreId = bestMatch.genre.firstOrNull()?.let {
                        genreDao.insertOrGetId(entity = it.toModel())
                    }

                    val coverId = bestMatch.cover?.let { dto ->
                        coverService.processCover(
                            coverDto = dto, rootUri = rootUri, folderId = current.id, mangaFolderName = current.name
                        )
                    }

                    remoteInfoDao.insert(
                        entity = bestMatch.toModel(
                            authorId = authorId, coverId = coverId, genreId = genreId
                        )
                    )
                }
            }

            result.onLeft { throwable ->
                when (throwable) {
                    is IntegrityException -> {
                        // TODO: Analisar alterações ou melhoras
                        println("SKIPPED [${current.name}] ${throwable.cause}")
                    }

                    is IOException, is MangadexRequestException -> {
                        // TODO: Analisar alterações ou melhoras
                        println("RECOVERABLE [${current.name}] ${throwable.cause}")
                    }

                    is SQLiteException -> {
                        throw throwable
                    }

                    else -> throw throwable
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
