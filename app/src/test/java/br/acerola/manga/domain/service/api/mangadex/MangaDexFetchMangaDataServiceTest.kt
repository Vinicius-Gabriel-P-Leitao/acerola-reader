package br.acerola.manga.domain.service.api.mangadex

import br.acerola.manga.domain.data.dao.api.mangadex.FakeMangadexMetadataMangaDao
import br.acerola.manga.data.remote.mangadex.dto.manga.MangaAttributes
import br.acerola.manga.data.remote.mangadex.dto.manga.MangaMangadexDto
import br.acerola.manga.data.remote.mangadex.dto.MangaDexResponse
import br.acerola.manga.shared.error.exception.MangadexRequestException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class MangaDexFetchMangaDataServiceTest {

    @Test
    fun searchManga_success_returnsList() = runBlocking {
        val fakeDao = FakeMangadexMetadataMangaDao()
        fakeDao.response = MangaDexResponse(
            result = "ok", response = "collection",
            data = listOf(
                MangaMangadexDto(
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

        val service = MangadexFetchMangaDataService(fakeDao)
        val result = service.searchMetadata("One Piece", 10, 0)

        assertEquals(1, result.size)
        assertEquals("One Piece", result[0].title)
    }

    @Test
    fun searchManga_httpError_throwsRequestError() = runBlocking {
        val fakeDao = FakeMangadexMetadataMangaDao()
        fakeDao.shouldThrow = true

        val service = MangadexFetchMangaDataService(fakeDao)

        try {
            service.searchMetadata("Unknown", 10, 0)
            fail("Should have thrown MangaDexRequestError")
        } catch (mangaDexRequestException: MangadexRequestException) {
            // Success
        }
    }
}
