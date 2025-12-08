package br.acerola.manga.domain.service.api.mangadex

import br.acerola.manga.domain.data.dao.api.mangadex.FakeMangaDataMangaDexDao
import br.acerola.manga.shared.dto.mangadex.MangaAttributes
import br.acerola.manga.shared.dto.mangadex.MangaData
import br.acerola.manga.shared.dto.mangadex.MangaDexResponse
import br.acerola.manga.shared.error.exception.MangaDexRequestError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class MangaDexFetchMangaDataServiceTest {

    @Test
    fun searchManga_success_returnsList() = runBlocking {
        val fakeDao = FakeMangaDataMangaDexDao()
        fakeDao.response = MangaDexResponse(
            result = "ok", response = "collection",
            data = listOf(
                MangaData(
                    id = "1", type = "manga",
                    attributes = MangaAttributes(
                        titleMap = mapOf("en" to "One Piece"),
                        status = "ongoing",
                        links = null
                    )
                )
            ),
            limit = 10, offset = 0, total = 1
        )

        val service = MangaDexFetchMangaDataService(fakeDao)
        val result = service.searchManga("One Piece", 10, 0)

        assertEquals(1, result.size)
        assertEquals("One Piece", result[0].title)
    }

    @Test
    fun searchManga_httpError_throwsRequestError() = runBlocking {
        val fakeDao = FakeMangaDataMangaDexDao()
        fakeDao.shouldThrow = true

        val service = MangaDexFetchMangaDataService(fakeDao)

        try {
            service.searchManga("Unknown", 10, 0)
            fail("Should have thrown MangaDexRequestError")
        } catch (mangaDexRequestError: MangaDexRequestError) {
            // Success
        }
    }
}
