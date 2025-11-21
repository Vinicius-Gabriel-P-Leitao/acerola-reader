package br.acerola.manga.domain.service.library.sync

import android.net.Uri
import android.util.Log
import br.acerola.manga.BuildConfig
import br.acerola.manga.domain.database.dao.database.archive.MangaFolderDao
import br.acerola.manga.domain.database.dao.database.metadata.MangaMetadataDao
import br.acerola.manga.domain.mapper.toModel
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.domain.service.mangadex.FetchMangaDataMangaDexService
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import br.acerola.manga.shared.error.MangaDexRequestError
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class SyncMetadataMangaService(
    private val mangaDao: MangaMetadataDao,
    private val folderDao: MangaFolderDao,
    private val fetchManga: FetchMangaDataMangaDexService = FetchMangaDataMangaDexService()
) : LibraryPort<MangaMetadataDto> {
    private val _progress = MutableStateFlow(value = -1)
    override val progress: StateFlow<Int> = _progress

    private val _mangas = MutableStateFlow<List<MangaMetadataDto>>(value = emptyList())
    val mangas: StateFlow<List<MangaMetadataDto>> get() = _mangas

    // TODO: Tratar erros melhor
    override suspend fun syncMangas(baseUri: Uri?) = withContext(context = Dispatchers.IO) {
        val folders = folderDao.getAllMangasFolders().firstOrNull() ?: emptyList()
        val titles = folders.map { it.name }

        val total = titles.size
        if (total == 0) {
            _progress.value = -1
            return@withContext
        }

        val updatedList = mutableListOf<MangaMetadataDto>()
        titles.forEachIndexed { _, title ->
            try {
                val existingMetadata = mangaDao.getMangaMetadataByName(name = title).firstOrNull()

                val fetchedList = fetchManga.searchManga(title = title)
                val firstResult = fetchedList.firstOrNull() ?: return@forEachIndexed
                val newModel = firstResult.toModel()

                if (existingMetadata != null) {
                    val dataToUpdate = newModel.copy(id = existingMetadata.id)
                    mangaDao.updateMangaMetadata(manga = dataToUpdate)
                    updatedList.add(firstResult)
                    return@withContext
                }

                mangaDao.insertMangaMetadata(manga = firstResult.toModel())
                updatedList.add(firstResult)
            } catch (mangaDexRequestError: MangaDexRequestError) {
                throw mangaDexRequestError
            } catch (exception: Exception) {
                // TODO: Criar string
                throw MangaDexRequestError(
                    title = "Erro ao sincronizar metadados",
                    description = exception.message ?: "Falha desconhecida no serviço de sincronização."
                )
            }
        }

        _mangas.value = updatedList
        _progress.value = -1
    }

    override suspend fun rescanMangas(baseUri: Uri?) {
        TODO("Not yet implemented")
    }

    override suspend fun deepRescanLibrary(baseUri: Uri?) {
        TODO("Not yet implemented")
    }
}