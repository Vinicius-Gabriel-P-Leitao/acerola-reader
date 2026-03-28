package br.acerola.manga.adapter.metadata.comicinfo.engine

import android.database.sqlite.SQLiteException
import android.net.Uri
import arrow.core.Either
import br.acerola.manga.adapter.contract.gateway.ChapterGateway
import br.acerola.manga.adapter.contract.provider.MetadataProvider
import br.acerola.manga.adapter.metadata.comicinfo.ComicInfoSource
import br.acerola.manga.dto.metadata.chapter.ChapterMetadataDto
import br.acerola.manga.dto.metadata.chapter.ChapterRemoteInfoPageDto
import br.acerola.manga.error.message.LibrarySyncError
import br.acerola.manga.local.dao.archive.ChapterArchiveDao
import br.acerola.manga.local.dao.archive.MangaDirectoryDao
import br.acerola.manga.local.dao.metadata.ChapterDownloadSourceDao
import br.acerola.manga.local.dao.metadata.ChapterMetadataDao
import br.acerola.manga.local.dao.metadata.MangaMetadataDao
import br.acerola.manga.local.translator.persistence.toDownloadSourcesEntities
import br.acerola.manga.local.translator.persistence.toEntity
import br.acerola.manga.logging.AcerolaLogger
import br.acerola.manga.logging.LogSource
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
    private val directoryDao: MangaDirectoryDao,
    private val chapterArchiveDao: ChapterArchiveDao,
    private val mangaMetadataDao: MangaMetadataDao,
    private val chapterMetadataDao: ChapterMetadataDao,
    private val chapterDownloadSourceDao: ChapterDownloadSourceDao,
) : ChapterGateway<ChapterRemoteInfoPageDto> {

    @Inject
    @ComicInfoSource
    lateinit var comicInfoSourceService: MetadataProvider<ChapterMetadataDto, String>

    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun refreshMangaChapters(mangaId: Long, baseUri: Uri?): Either<LibrarySyncError, Unit> =
        withContext(context = Dispatchers.IO) {
            AcerolaLogger.i(TAG, "Starting ComicInfo metadata sync for chapter archives of manga: $mangaId", LogSource.REPOSITORY)
            _isIndexing.value = true
            _progress.value = 0

            Either.catch {
                val directory = directoryDao.getMangaDirectoryById(mangaId)
                    ?: throw Exception("Directory not found for ID: $mangaId")

                val remoteManga = mangaMetadataDao.getMangaByDirectoryId(directory.id).first()
                    ?: throw Exception("Remote info not found for manga directory: ${directory.name}")

                val localChapters = chapterArchiveDao.getChaptersByMangaDirectory(directory.id).first()

                val total = localChapters.size
                if (total == 0) {
                    AcerolaLogger.d(TAG, "No local chapters found for manga: ${directory.name}", LogSource.REPOSITORY)
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

                AcerolaLogger.i(TAG, "Finished ComicInfo sync for manga: ${directory.name}", LogSource.REPOSITORY)
                _progress.value = 100
            }.mapLeft { exception ->
                AcerolaLogger.e(TAG, "ComicInfo chapter sync failed for manga: $mangaId", LogSource.REPOSITORY, throwable = exception)
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

    override fun observeSpecificChapters(mangaId: Long, chapters: List<String>): Flow<ChapterRemoteInfoPageDto> {
        return flowOf(
            value = ChapterRemoteInfoPageDto(items = emptyList(), pageSize = 0, page = 0, total = 0)
        )
    }

    override suspend fun getChapterPage(mangaId: Long, total: Int, page: Int, pageSize: Int): ChapterRemoteInfoPageDto {
        return ChapterRemoteInfoPageDto(items = emptyList(), pageSize, page, total)
    }

    companion object {
        private const val TAG = "ComicInfoChapterRepository"
    }
}
