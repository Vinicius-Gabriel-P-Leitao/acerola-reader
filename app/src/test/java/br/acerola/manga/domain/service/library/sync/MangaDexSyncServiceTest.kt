package br.acerola.manga.domain.service.library.sync

import android.content.Context
import android.net.Uri
import br.acerola.manga.domain.data.dao.api.mangadex.FakeMangaDataMangaDexDao
import br.acerola.manga.domain.data.dao.database.FakeMangaFolderDao
import br.acerola.manga.domain.data.dao.database.FakeMangaMetadataDao
import br.acerola.manga.domain.data.dao.database.metadata.author.AuthorDao
import br.acerola.manga.domain.data.dao.database.metadata.gender.GenderDao
import br.acerola.manga.domain.model.archive.MangaFolder
import br.acerola.manga.domain.model.metadata.author.Author
import br.acerola.manga.domain.model.metadata.gender.Gender
import br.acerola.manga.domain.service.api.mangadex.MangaDexFetchMangaDataService
import br.acerola.manga.domain.service.archive.MangaCoverService
import br.acerola.manga.shared.config.preference.FolderPreference
import br.acerola.manga.shared.dto.mangadex.MangaAttributes
import br.acerola.manga.shared.dto.mangadex.MangaData
import br.acerola.manga.shared.dto.mangadex.MangaDexResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MangaDexSyncServiceTest {
    private lateinit var context: Context
    private lateinit var fakeAuthorDao: FakeAuthorDao
    private lateinit var fakeGenderDao: FakeGenderDao
    private lateinit var fakeFolderDao: FakeMangaFolderDao
    private lateinit var fakeMangaMetadataDao: FakeMangaMetadataDao
    private lateinit var fakeMangaCoverService: MangaCoverService
    private lateinit var fakeFetchMangaDataService: MangaDexFetchMangaDataService
    private lateinit var fakeMangaDexDao: FakeMangaDataMangaDexDao

    private lateinit var service: MangaDexSyncService

    class FakeAuthorDao : AuthorDao {
        val authors = mutableListOf<Author>()
        override suspend fun insert(entity: Author): Long {
            authors.add(entity); return entity.id
        }

        override suspend fun insertAll(vararg entity: Author) {
            authors.addAll(entity)
        }

        override suspend fun update(entity: Author) {}
        override suspend fun delete(entity: Author) {}
        override suspend fun getAuthorByMirrorId(mirrorId: String): Author? = authors.find { it.mirrorId == mirrorId }
    }

    class FakeGenderDao : GenderDao {
        val genders = mutableListOf<Gender>()
        override suspend fun insert(entity: Gender): Long {
            genders.add(entity); return entity.id
        }

        override suspend fun insertAll(vararg entity: Gender) {
            genders.addAll(entity)
        }

        override suspend fun update(entity: Gender) {}
        override suspend fun delete(entity: Gender) {}
        override suspend fun getGenderByMirrorId(mirrorId: String): Gender? = genders.find { it.mirrorId == mirrorId }
    }

    @Before
    fun setup() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers {
            val arg = firstArg<String>()
            val mockUri = mockk<Uri>()
            every { mockUri.toString() } returns arg
            mockUri
        }

        context = mockk(relaxed = true)
        fakeAuthorDao = FakeAuthorDao()
        fakeGenderDao = FakeGenderDao()
        fakeFolderDao = FakeMangaFolderDao()
        fakeMangaMetadataDao = FakeMangaMetadataDao()
        fakeMangaCoverService = mockk(relaxed = true)

        fakeMangaDexDao = FakeMangaDataMangaDexDao()
        fakeFetchMangaDataService = MangaDexFetchMangaDataService(fakeMangaDexDao)

        service = MangaDexSyncService(
            context,
            fakeAuthorDao,
            fakeGenderDao,
            fakeFolderDao,
            fakeMangaMetadataDao,
            fakeMangaCoverService,
            fakeFetchMangaDataService
        )

        mockkObject(FolderPreference)
        every { FolderPreference.folderUriFlow(any()) } returns flowOf("content://tree")
    }

    @After
    fun tearDown() {
        unmockkObject(FolderPreference)
        unmockkStatic(Uri::class)
    }

    @Test
    fun syncMangas_syncsNewManga() = runBlocking {
        val folder = MangaFolder(
            id = 1,
            name = "One Piece",
            path = "path",
            cover = null,
            banner = null,
            lastModified = 0,
            chapterTemplate = null
        )

        fakeFolderDao.folders.add(folder)
        fakeMangaDexDao.response = MangaDexResponse(
            result = "ok", response = "collection", data = listOf(
                MangaData(
                    id = "mp1", type = "manga", attributes = MangaAttributes(
                        titleMap = mapOf("en" to "One Piece"), status = "ongoing", links = null
                    )
                )
            ), limit = 1, offset = 0, total = 1
        )

        service.syncMangas(null)

        assertEquals(1, fakeMangaMetadataDao.metadataList.size)
        assertEquals("One Piece", fakeMangaMetadataDao.metadataList[0].name)
    }
}
