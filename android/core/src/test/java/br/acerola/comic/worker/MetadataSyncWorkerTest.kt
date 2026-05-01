package br.acerola.comic.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import arrow.core.Either
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.usecase.library.SyncLibraryUseCase
import br.acerola.comic.usecase.metadata.SyncComicMetadataUseCase
import br.acerola.comic.worker.sync.MetadataSyncWorker
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetadataSyncWorkerTest {
    private lateinit var context: Context

    @MockK
    lateinit var syncComicMetadataUseCase: SyncComicMetadataUseCase

    @MockK
    lateinit var anilistSyncUseCase: SyncLibraryUseCase

    @MockK
    lateinit var mangadexSyncUseCase: SyncLibraryUseCase

    @MockK
    lateinit var comicInfoSyncUseCase: SyncLibraryUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()
        every { anilistSyncUseCase.progress } returns MutableStateFlow(0)
        every { mangadexSyncUseCase.progress } returns MutableStateFlow(0)
        every { comicInfoSyncUseCase.progress } returns MutableStateFlow(0)
    }

    private fun buildWorker(inputData: Data): MetadataSyncWorker =
        TestListenableWorkerBuilder<MetadataSyncWorker>(context)
            .setInputData(inputData)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ) = MetadataSyncWorker(
                        appContext,
                        workerParameters,
                        syncComicMetadataUseCase,
                        anilistSyncUseCase,
                        mangadexSyncUseCase,
                        comicInfoSyncUseCase,
                    )
                },
            ).build()

    // Test for directoryId != -1L (Single comic sync)
    @Test
    fun `doWork should return success when syncFromMangadex for single comic succeeds`() =
        runBlocking {
            coEvery { syncComicMetadataUseCase.syncFromMangadex(42L) } returns Either.Right(Unit)

            val worker =
                buildWorker(
                    workDataOf(
                        MetadataSyncWorker.KEY_SYNC_SOURCE to MetadataSyncWorker.SOURCE_MANGADEX,
                        MetadataSyncWorker.KEY_DIRECTORY_ID to 42L,
                    ),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Success)
        }

    @Test
    fun `doWork should return failure when syncFromMangadex for single comic fails`() =
        runBlocking {
            coEvery { syncComicMetadataUseCase.syncFromMangadex(42L) } returns
                Either.Left(
                    LibrarySyncError.SyncNetworkError(),
                )

            val worker =
                buildWorker(
                    workDataOf(
                        MetadataSyncWorker.KEY_SYNC_SOURCE to MetadataSyncWorker.SOURCE_MANGADEX,
                        MetadataSyncWorker.KEY_DIRECTORY_ID to 42L,
                    ),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Failure)
        }

    @Test
    fun `doWork should return success when syncFromAnilist for single comic succeeds`() =
        runBlocking {
            coEvery { syncComicMetadataUseCase.syncFromAnilist(42L) } returns Either.Right(Unit)

            val worker =
                buildWorker(
                    workDataOf(
                        MetadataSyncWorker.KEY_SYNC_SOURCE to MetadataSyncWorker.SOURCE_ANILIST,
                        MetadataSyncWorker.KEY_DIRECTORY_ID to 42L,
                    ),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Success)
        }

    @Test
    fun `doWork should return success when syncFromComicInfo for single comic succeeds`() =
        runBlocking {
            coEvery { syncComicMetadataUseCase.syncFromComicInfo(42L) } returns Either.Right(Unit)

            val worker =
                buildWorker(
                    workDataOf(
                        MetadataSyncWorker.KEY_SYNC_SOURCE to MetadataSyncWorker.SOURCE_COMICINFO,
                        MetadataSyncWorker.KEY_DIRECTORY_ID to 42L,
                    ),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Success)
        }

    // Test for Library-wide sync
    @Test
    fun `doWork should return success when mangadex library sync succeeds`() =
        runBlocking {
            coEvery { mangadexSyncUseCase.sync(null) } returns Either.Right(Unit)

            val worker =
                buildWorker(
                    workDataOf(
                        MetadataSyncWorker.KEY_SYNC_SOURCE to MetadataSyncWorker.SOURCE_MANGADEX,
                        MetadataSyncWorker.KEY_SYNC_TYPE to MetadataSyncWorker.SYNC_TYPE_SYNC,
                    ),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Success)
        }

    @Test
    fun `doWork should return failure when mangadex library sync fails`() =
        runBlocking {
            coEvery { mangadexSyncUseCase.sync(null) } returns
                Either.Left(
                    LibrarySyncError.SyncNetworkError(),
                )

            val worker =
                buildWorker(
                    workDataOf(
                        MetadataSyncWorker.KEY_SYNC_SOURCE to MetadataSyncWorker.SOURCE_MANGADEX,
                        MetadataSyncWorker.KEY_SYNC_TYPE to MetadataSyncWorker.SYNC_TYPE_SYNC,
                    ),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Failure)
        }

    @Test
    fun `doWork should return success when anilist library rescan succeeds`() =
        runBlocking {
            coEvery { anilistSyncUseCase.rescan(null) } returns Either.Right(Unit)

            val worker =
                buildWorker(
                    workDataOf(
                        MetadataSyncWorker.KEY_SYNC_SOURCE to MetadataSyncWorker.SOURCE_ANILIST,
                        MetadataSyncWorker.KEY_SYNC_TYPE to MetadataSyncWorker.SYNC_TYPE_RESCAN,
                    ),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Success)
        }

    @Test
    fun `doWork should return success when comicInfo library sync succeeds`() =
        runBlocking {
            coEvery { comicInfoSyncUseCase.sync(null) } returns Either.Right(Unit)

            val worker =
                buildWorker(
                    workDataOf(
                        MetadataSyncWorker.KEY_SYNC_SOURCE to MetadataSyncWorker.SOURCE_COMICINFO,
                        MetadataSyncWorker.KEY_SYNC_TYPE to MetadataSyncWorker.SYNC_TYPE_SYNC,
                    ),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Success)
        }

    @Test
    fun `doWork should handle exceptions gracefully and return failure`() =
        runBlocking {
            coEvery { mangadexSyncUseCase.sync(null) } throws RuntimeException("Fatal error")

            val worker =
                buildWorker(
                    workDataOf(
                        MetadataSyncWorker.KEY_SYNC_SOURCE to MetadataSyncWorker.SOURCE_MANGADEX,
                        MetadataSyncWorker.KEY_SYNC_TYPE to MetadataSyncWorker.SYNC_TYPE_SYNC,
                    ),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Failure)
        }
}
