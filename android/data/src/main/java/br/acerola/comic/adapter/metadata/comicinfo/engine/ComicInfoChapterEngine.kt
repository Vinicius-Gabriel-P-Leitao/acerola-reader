package br.acerola.comic.adapter.metadata.comicinfo.engine

import android.database.sqlite.SQLiteException
import android.net.Uri
import arrow.core.Either
import br.acerola.comic.adapter.contract.gateway.ChapterGateway
import br.acerola.comic.adapter.contract.provider.MetadataProvider
import br.acerola.comic.adapter.metadata.comicinfo.ComicInfoSourceQualifier
import br.acerola.comic.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.comic.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.local.dao.archive.ChapterArchiveDao
import br.acerola.comic.local.dao.archive.ComicDirectoryDao
import br.acerola.comic.local.dao.metadata.ChapterDownloadSourceDao
import br.acerola.comic.local.dao.metadata.ChapterMetadataDao
import br.acerola.comic.local.dao.metadata.ComicMetadataDao
import br.acerola.comic.local.translator.persistence.toDownloadSourcesEntities
import br.acerola.comic.local.translator.persistence.toEntity
import br.acerola.comic.logging.AcerolaLogger
import br.acerola.comic.logging.LogSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComicInfoChapterEngine @Inject constructor(
    private val directoryDao: ComicDirectoryDao,
    private val chapterArchiveDao: ChapterArchiveDao,
    private val comicMetadataDao: ComicMetadataDao,
    private val chapterMetadataDao: ChapterMetadataDao,
    private val chapterDownloadSourceDao: ChapterDownloadSourceDao,
) : ChapterGateway<ChapterRemoteInfoPageDto> {

    @Inject
    @ComicInfoSourceQualifier
    lateinit var comicInfoSourceService: MetadataProvider<ChapterMetadataDto, String>

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun refreshComicChapters(mangaId: Long, baseUri: Uri?): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            AcerolaLogger.i(TAG, "Starting ComicInfo metadata sync for chapter archives of comic: $mangaId", LogSource.REPOSITORY)
            _isIndexing.value = true
            _progress.value = 0

            Either.catch {
                val directory = directoryDao.getDirectoryById(mangaId)
                    ?: throw Exception("Directory not found for ID: $mangaId")

                val remoteManga = comicMetadataDao.observeComicByDirectoryId(directory.id).first()
                    ?: throw Exception("Remote info not found for comic directory: ${directory.name}")

                val localChapters = chapterArchiveDao.getChaptersByDirectoryId(directory.id).first()

                val total = localChapters.size
                if (total == 0) {
                    AcerolaLogger.d(TAG, "No local chapters found for comic: ${directory.name}", LogSource.REPOSITORY)
                    return@catch
                }

                localChapters.forEachIndexed { index, archive ->
                    yield()
                    val result = comicInfoSourceService.searchInfo(manga = archive.path)
                        .getOrNull()
                        ?.firstOrNull()

                    if (result != null) {
                        AcerolaLogger.v(TAG, "Match found in ComicInfo for chapter: ${archive.chapter}", LogSource.REPOSITORY)
                        val chapterRemoteInfoEntity = result.toEntity(mangaRemoteInfoFk = remoteManga.id)
                        val chapterRemoteInfoId = chapterMetadataDao.insert(chapterRemoteInfoEntity)

                        val downloadSourceEntities = result.toDownloadSourcesEntities(chapterFk = chapterRemoteInfoId)
                        chapterDownloadSourceDao.insertAll(*downloadSourceEntities.toTypedArray())
                    }

                    _progress.value = ((index + 1).toFloat() / total.toFloat() * 100).toInt()
                }

                AcerolaLogger.i(TAG, "Finished ComicInfo sync for comic: ${directory.name}", LogSource.REPOSITORY)
                _progress.value = 100
            }.mapLeft { exception ->
                AcerolaLogger.e(TAG, "ComicInfo chapter sync failed for comic: $mangaId", LogSource.REPOSITORY, throwable = exception)
                when (exception) {
                    is SQLiteException -> LibrarySyncError.DatabaseError(cause = exception)
                    else -> LibrarySyncError.UnexpectedError(cause = exception)
                }
            }.also {
                _isIndexing.value = false
                _progress.value = -1
            }
        }

    override fun observeChapters(mangaId: Long): StateFlow<ChapterRemoteInfoPageDto> {
        return MutableStateFlow(
            value = ChapterRemoteInfoPageDto(items = emptyList(), pageSize = 0, page = 0, total = 0)
        ).asStateFlow()
    }

    override suspend fun getChapterPage(mangaId: Long, total: Int, page: Int, pageSize: Int): ChapterRemoteInfoPageDto {
        return ChapterRemoteInfoPageDto(items = emptyList(), pageSize, page, total)
    }

    companion object {
        private const val TAG = "ComicInfoChapterRepository"
    }
}
