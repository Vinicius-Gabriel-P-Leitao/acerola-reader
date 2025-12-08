package br.acerola.manga.domain.database.dao

import br.acerola.manga.domain.database.dao.api.mangadex.cover.MangaDexDownloadDao
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import retrofit2.http.Url

class FakeMangaDexDownloadDao : MangaDexDownloadDao {
    var responseBytes: ByteArray = ByteArray(0)
    var shouldThrow: Boolean = false
    var lastUrl: String? = null

    override suspend fun downloadFile(@Url fileUrl: String): ResponseBody {
        lastUrl = fileUrl
        if (shouldThrow) throw RuntimeException("Fake error")
        return ResponseBody.create("image/png".toMediaTypeOrNull(), responseBytes)
    }
}
