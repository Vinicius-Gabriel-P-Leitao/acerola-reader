package br.acerola.manga.domain.service.api.mangadex

import br.acerola.manga.domain.database.dao.FakeMangaDexDownloadDao
import br.acerola.manga.shared.error.exception.MangaDexRequestError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.fail
import org.junit.Test

class MangaDexFetchCoverServiceTest {

    @Test
    fun searchCover_success_returnsBytes() = runBlocking {
        val expectedBytes = byteArrayOf(1, 2, 3)
        val fakeDao = FakeMangaDexDownloadDao()
        fakeDao.responseBytes = expectedBytes

        val service = MangaDexFetchCoverService(fakeDao)
        val result = service.searchCover("http://example.com/cover.png")

        assertArrayEquals(expectedBytes, result)
    }

    @Test
    fun searchCover_error_throwsMangaDexRequestError() = runBlocking {
        val fakeDao = FakeMangaDexDownloadDao()
        fakeDao.shouldThrow = true

        val service = MangaDexFetchCoverService(fakeDao)

        try {
            service.searchCover("http://example.com/cover.png")
            fail("Should have thrown MangaDexRequestError")
        } catch (_: MangaDexRequestError) {
        } catch (exception: Exception) {
            fail("Should have thrown MangaDexRequestError, but threw ${exception::class.simpleName}")
        }
    }
}
