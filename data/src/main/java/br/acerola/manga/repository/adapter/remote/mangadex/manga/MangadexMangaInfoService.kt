package br.acerola.manga.repository.adapter.remote.mangadex.manga

import android.content.Context
import arrow.core.Either
import br.acerola.manga.data.R
import br.acerola.manga.dto.metadata.manga.AuthorDto
import br.acerola.manga.dto.metadata.manga.CoverDto
import br.acerola.manga.dto.metadata.manga.GenreDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.error.message.NetworkError
import br.acerola.manga.local.mapper.toDto
import br.acerola.manga.network.safeApiCall
import br.acerola.manga.remote.mangadex.api.MangadexMangaInfoApi
import br.acerola.manga.repository.port.ApiRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton

class MangadexMangaInfoService @Inject constructor(
    @param:ApplicationContext private val context: Context, private val api: MangadexMangaInfoApi
) : ApiRepository.RemoteInfoOperations<MangaRemoteInfoDto, String> {

    // TODO: Criar um progresso simples, esse progresso vai ser só pra saber que isso iniciou
    override suspend fun searchInfo(
        manga: String, limit: Int, offset: Int, onProgress: ((Int) -> Unit)?, vararg extra: String?
    ): Either<NetworkError, List<MangaRemoteInfoDto>> = safeApiCall {
        withContext(context = Dispatchers.IO) {
            val response = api.searchMangaByName(title = manga, limit = limit, offset = offset)
            response.data.map { it.toDto(context) }
        }
    }
}