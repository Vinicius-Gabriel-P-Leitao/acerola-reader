package br.acerola.manga.domain.service.library.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import br.acerola.manga.R
import br.acerola.manga.domain.database.dao.database.archive.MangaFolderDao
import br.acerola.manga.domain.database.dao.database.metadata.MangaMetadataDao
import br.acerola.manga.domain.database.dao.database.metadata.author.AuthorDao
import br.acerola.manga.domain.database.dao.database.metadata.gender.GenderDao
import br.acerola.manga.domain.mapper.toModel
import br.acerola.manga.domain.model.metadata.author.Author
import br.acerola.manga.domain.model.metadata.author.TypeAuthor
import br.acerola.manga.domain.model.metadata.gender.Gender
import br.acerola.manga.domain.service.api.mangadex.MangaDexFetchMangaDataService
import br.acerola.manga.domain.service.archive.MangaCoverService
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.shared.config.preference.FolderPreference
import br.acerola.manga.shared.dto.metadata.AuthorDto
import br.acerola.manga.shared.dto.metadata.GenreDto
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import br.acerola.manga.shared.error.exception.MangaDexRequestError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.core.net.toUri

// TODO: Criar um método privado que vai chamar um futuro serviço
//  FetchCoverMangaDexService e escrever o arquivo de resultado da API no sistema de arquivos
class MangaDexSyncService(
    private val context: Context,
    private val authorDao: AuthorDao,
    private val genderDao: GenderDao,
    private val folderDao: MangaFolderDao,
    private val mangaDao: MangaMetadataDao,
    private val mangaCoverService: MangaCoverService,
    private val fetchManga: MangaDexFetchMangaDataService = MangaDexFetchMangaDataService(),
) : LibraryPort<MangaMetadataDto> {
    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress.asStateFlow()

    // TODO: Tratar erros melhor
    // TODO: Sync de dados simples, vai buscar de apenas dados novos, caso do DB de folder tenha um que não existe ainda no de metadados
    //  ele vai fazer um scan só para ele
    override suspend fun syncMangas(baseUri: Uri?) = withContext(context = Dispatchers.IO) {
        val allFolders = folderDao.getAllMangasFolders().firstOrNull() ?: emptyList()
        val allMetadata = mangaDao.getAllMangasMetadata().firstOrNull() ?: emptyList()

        val existingTitles = allMetadata.map {
            it.name.filter { char -> char.isLetterOrDigit() }.lowercase()
        }.toSet()

        val metadataToSync = allFolders.filter { folder ->
            val normalizedName = folder.name.filter { char -> char.isLetterOrDigit() }.lowercase()
            normalizedName !in existingTitles
        }

        val total = metadataToSync.size
        if (total == 0) {
            _progress.value = -1
            return@withContext
        }

        _progress.value = 0

        metadataToSync.forEachIndexed { index, current ->
            val title = current.name

            val currentProgress = ((index.toFloat() / total.toFloat()) * 100).roundToInt()
            _progress.value = currentProgress

            if (index > 0) delay(timeMillis = 300)

            val rootPath = baseUri?.toString() ?: FolderPreference.folderUriFlow(context).firstOrNull()
            if (rootPath.isNullOrBlank()) {
                _progress.value = -1
                return@withContext
            }

            val rootUri = rootPath.toUri()

            try {
                val fetchedList: List<MangaMetadataDto> = fetchManga.searchManga(title = title)
                val folderNameNormalized = title.filter { it.isLetterOrDigit() }.lowercase()

                val bestMatch: MangaMetadataDto? = fetchedList.find { candidate ->
                    val candidateTitleNormalized = candidate.title.filter { it.isLetterOrDigit() }.lowercase()
                    val candidateRomanjiNormalized = candidate.romanji?.filter { it.isLetterOrDigit() }?.lowercase()

                    candidateTitleNormalized == folderNameNormalized || candidateRomanjiNormalized == folderNameNormalized
                } ?: fetchedList.firstOrNull()

                if (bestMatch == null) return@forEachIndexed

                // NOTE: Popula o banco de dados para poder ter mais metadados
                val authorId: Long? = bestMatch.authors?.let { authorDto ->
                    saveAndGetAuthorId(authorDto)
                }

                val genderId: Long? = bestMatch.gender.firstOrNull()?.let { genreDto ->
                    saveAndGetGenderId(genreDto)
                }

                val coverId: Long? = bestMatch.cover?.let { dto ->
                    mangaCoverService.processCover(
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
            } catch (mangaDexRequestError: MangaDexRequestError) {
                throw mangaDexRequestError
            } catch (exception: Exception) {
                println(exception)
                throw MangaDexRequestError(
                    title = R.string.title_error_mangadex_sync,
                    description = R.string.message_error_mangadex_sync_unknown
                )
            }
        }

        _progress.value = 100
        delay(timeMillis = 250)
        _progress.value = -1
    }

    // TODO: Fazer reescan bruto de metados onde vai refazer todas buscas, porem só de mangás
    override suspend fun rescanMangas(baseUri: Uri?) {
        TODO("Not yet implemented")
    }

    // TODO: Fazer uma busca mais bruta ainda, vai buscar tando dos mangas quando dos capitulos
    override suspend fun deepRescanLibrary(baseUri: Uri?) {
        TODO("Not yet implemented")
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
}