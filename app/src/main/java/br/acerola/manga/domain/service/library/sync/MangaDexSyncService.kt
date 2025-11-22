package br.acerola.manga.domain.service.library.sync

import android.net.Uri
import br.acerola.manga.R
import br.acerola.manga.domain.database.dao.database.archive.MangaFolderDao
import br.acerola.manga.domain.database.dao.database.metadata.MangaMetadataDao
import br.acerola.manga.domain.mapper.toModel
import br.acerola.manga.domain.service.api.mangadex.MangaDexFetchMangaDataService
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import br.acerola.manga.shared.error.exception.MangaDexRequestError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// TODO: Criar um método privado que vai chamar um futuro serviço
//  FetchCoverMangaDexService e escrever o arquivo de resultado da API no sistema de arquivos
class MangaDexSyncService(
    private val folderDao: MangaFolderDao,
    private val mangaDao: MangaMetadataDao,
    private val fetchManga: MangaDexFetchMangaDataService = MangaDexFetchMangaDataService(),
) : LibraryPort<MangaMetadataDto> {
    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress

    private val _mangas = MutableStateFlow<List<MangaMetadataDto>>(value = emptyList())
    val mangas: StateFlow<List<MangaMetadataDto>> get() = _mangas

    // TODO: Tratar erros melhor
    // TODO: Sync de dados simples, vai buscar de apenas dados novos, caso do DB de folder tenha um que não existe ainda no de metadados
    //  ele vai fazer um scan só para ele
    override suspend fun syncMangas(baseUri: Uri?) = withContext(context = Dispatchers.IO) {
        _progress.value = 0

        val folders = folderDao.getAllMangasFolders().firstOrNull() ?: emptyList()
        val titles = folders.map { it.name }

        val total = titles.size
        if (total == 0) {
            _progress.value = 100
            return@withContext
        }

        val updatedList = mutableListOf<MangaMetadataDto>()
        titles.forEachIndexed { index, title ->
            val currentProgress = ((index.toFloat() / total.toFloat()) * 100).roundToInt()
            _progress.value = currentProgress

            if (index > 0) delay(timeMillis = 300)

            try {
                val existingMetadata = mangaDao.getMangaMetadataByName(name = title).firstOrNull()

                val fetchedList: List<MangaMetadataDto> = fetchManga.searchManga(title = title)
                val folderNameNormalized = title.filter { it.isLetterOrDigit() }.lowercase()

                val bestMatch: MangaMetadataDto? = fetchedList.find { candidate ->
                    val candidateTitleNormalized = candidate.title.filter { it.isLetterOrDigit() }.lowercase()
                    val candidateRomanjiNormalized = candidate.romanji?.filter { it.isLetterOrDigit() }?.lowercase()

                    candidateTitleNormalized == folderNameNormalized || candidateRomanjiNormalized == folderNameNormalized
                } ?: fetchedList.firstOrNull()

                if (bestMatch == null) return@forEachIndexed

                val newModel = bestMatch.toModel()

                if (existingMetadata != null) {
                    val dataToUpdate = newModel.copy(id = existingMetadata.id)
                    mangaDao.updateMangaMetadata(manga = dataToUpdate)
                    updatedList.add(bestMatch)
                    return@withContext
                }

                mangaDao.insertMangaMetadata(manga = newModel)
                updatedList.add(bestMatch)
            } catch (mangaDexRequestError: MangaDexRequestError) {
                throw mangaDexRequestError
            } catch (_: Exception) {
                // TODO: Criar string
                throw MangaDexRequestError(
                    title = R.string.title_error_mangadex_sync,
                    description = R.string.message_error_mangadex_sync_unknown
                )
            }
        }

        _mangas.value = updatedList
        _progress.value = 100
    }

    // TODO: Fazer reescan bruto de metados onde vai refazer todas buscas, porem só de mangás
    override suspend fun rescanMangas(baseUri: Uri?) {
        TODO("Not yet implemented")
    }

    // TODO: Fazer uma busca mais bruta ainda, vai buscar tando dos mangas quando dos capitulos
    override suspend fun deepRescanLibrary(baseUri: Uri?) {
        TODO("Not yet implemented")
    }
}