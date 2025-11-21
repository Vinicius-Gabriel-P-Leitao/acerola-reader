package br.acerola.manga.domain.service.library.sync

import android.net.Uri
import androidx.annotation.Nullable
import br.acerola.manga.BuildConfig
import br.acerola.manga.domain.database.dao.database.archive.MangaFolderDao
import br.acerola.manga.domain.database.dao.database.metadata.MangaMetadataDao
import br.acerola.manga.domain.mapper.toModel
import br.acerola.manga.domain.service.library.LibraryPort
import br.acerola.manga.domain.service.mangadex.FetchMangaDataMangaDexService
import br.acerola.manga.shared.dto.metadata.MangaMetadataDto
import br.acerola.manga.shared.error.MangaDexRequestError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext


class SyncMetadataMangaService(
    private val mangaDao: MangaMetadataDao,
    private val folderDao: MangaFolderDao,
    private val fetchManga: FetchMangaDataMangaDexService = FetchMangaDataMangaDexService(BuildConfig.MANGADEX_BASE_URL)
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
        titles.forEachIndexed { index, title ->
            val uri = Uri.parse(BuildConfig.MANGADEX_BASE_URL).buildUpon().appendPath("manga")
                .appendQueryParameter("title", title).build()

            try {
                val fetchedList = fetchManga.searchManga(uri)
                val firstResult = fetchedList.firstOrNull() ?: return@forEachIndexed

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