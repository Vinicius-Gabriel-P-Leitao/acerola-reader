package br.acerola.manga.domain.service.library.sync

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import br.acerola.manga.domain.data.dao.database.archive.MangaFolderDao
import br.acerola.manga.domain.data.dao.database.metadata.MangaMetadataDao
import br.acerola.manga.domain.data.dao.database.metadata.author.AuthorDao
import br.acerola.manga.domain.data.dao.database.metadata.gender.GenderDao
import br.acerola.manga.domain.data.mapper.toModel
import br.acerola.manga.domain.model.metadata.author.Author
import br.acerola.manga.domain.model.metadata.author.TypeAuthor
import br.acerola.manga.domain.model.metadata.gender.Gender
import br.acerola.manga.domain.service.api.mangadex.MangadexFetchMangaDataService
import br.acerola.manga.domain.service.archive.MangaCoverService
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.shared.config.preference.FolderPreference
import br.acerola.manga.shared.dto.metadata.AuthorDto
import br.acerola.manga.shared.dto.metadata.GenreDto
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
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
    @param:ApplicationContext private val context: Context,
    private val authorDao: AuthorDao,
    private val genderDao: GenderDao,
    private val folderDao: MangaFolderDao,
    private val mangaDao: MangaMetadataDao,
    private val coverService: MangaCoverService,
    private val fetchManga: MangadexFetchMangaDataService,
) : LibraryPort<MangaMetadataDto> {
    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isIndexing = MutableStateFlow(value = false)
    override val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    override suspend fun syncMangas(baseUri: Uri?) {
        _isIndexing.value = true

        try {
            withContext(context = Dispatchers.IO) {
                val allFolders = folderDao.getAllMangasFolders().firstOrNull() ?: emptyList()
                val allMetadata = mangaDao.getAllMangasMetadata().firstOrNull() ?: emptyList()

                val existingTitles = allMetadata.map { normalizeName(it.name) }.toSet()

                val metadataToSync = allFolders.filter { folder ->
                    normalizeName(folder.name) !in existingTitles
                }

                if (metadataToSync.isEmpty()) {
                    _progress.value = -1
                    return@withContext
                }

                executeSync(folders = metadataToSync, baseUri)
            }
        } finally {
            _isIndexing.value = false
        }
    }

    override suspend fun rescanMangas(baseUri: Uri?) {
        _isIndexing.value = true
        try {
            withContext(context = Dispatchers.IO) {
                val allFolders = folderDao.getAllMangasFolders().firstOrNull() ?: emptyList()

                if (allFolders.isEmpty()) {
                    _progress.value = -1
                    return@withContext
                }

                executeSync(folders = allFolders, baseUri)
            }
        } finally {
            _isIndexing.value = false
        }
    }

    // NOTE: Pensar em forma de fazer isso futuramente, mas até agora é função inutil
    override suspend fun deepRescanLibrary(baseUri: Uri?) {
        rescanMangas(baseUri)
    }

    private suspend fun executeSync(folders: List<br.acerola.manga.domain.model.archive.MangaFolder>, baseUri: Uri?) {
        val total = folders.size
        _progress.value = 0

        // NOTE: Se a URL não for passada ele usa a de preferencia.
        val rootPath = baseUri?.toString() ?: FolderPreference.folderUriFlow(context).firstOrNull()
        if (rootPath.isNullOrBlank()) {
            _progress.value = -1
            return
        }
        val rootUri = rootPath.toUri()

        folders.forEachIndexed { index, current ->
            try {
                val title = current.name
                val fetchedList: List<MangaMetadataDto> = fetchManga.searchManga(title = title)
                val folderNameNormalized = normalizeName(name = title)

                val bestMatch: MangaMetadataDto? = fetchedList.find { candidate ->
                    normalizeName(name = candidate.title) == folderNameNormalized || normalizeName(
                        name = candidate.romanji ?: ""
                    ) == folderNameNormalized
                } ?: fetchedList.firstOrNull()

                if (bestMatch != null) {
                    val authorId = bestMatch.authors?.let { saveAndGetAuthorId(dto = it) }
                    val genderId = bestMatch.gender.firstOrNull()?.let { saveAndGetGenderId(dto = it) }

                    val coverId = bestMatch.cover?.let { dto ->
                        coverService.processCover(
                            coverDto = dto,
                            rootUri = rootUri,
                            folderId = current.id,
                            mangaFolderName = current.name,
                        )
                    }

                    val newMangaEntity = bestMatch.toModel(
                        authorId = authorId, coverId = coverId, genderId = genderId
                    )

                    mangaDao.insert(entity = newMangaEntity)
                }

            } catch (exception: Exception) {
                // TODO: Normalizar esse erro, ao invez de um log
                println("Erro ao sincronizar $current: ${exception.message}")
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
                mirrorId = dto.id, name = dto.name, type = TypeAuthor.getByType(dto.type)
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

    private suspend fun saveAndGetGenderId(dto: GenreDto): Long {
        val insertedId = genderDao.insert(
            entity = Gender(
                mirrorId = dto.id, gender = dto.name
            )
        )

        // TODO: Criar uma string e tratar erros melhor
        return if (insertedId != -1L) {
            insertedId
        } else {
            genderDao.getGenderByMirrorId(mirrorId = dto.id)?.id
                ?: throw IllegalStateException("Gênero deveria existir")
        }
    }

    private fun normalizeName(name: String): String {
        return name.filter { it.isLetterOrDigit() }.lowercase()
    }
}