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
import br.acerola.comic.pattern.metadata.MetadataSource
import br.acerola.comic.usecase.library.SyncLibraryUseCase
import br.acerola.comic.util.notification.NotificationHelper
import br.acerola.comic.worker.contract.SyncType
import br.acerola.comic.worker.sync.MetadataSyncWorker
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetadataSyncWorkerTest {
    private lateinit var context: Context

    @MockK
    lateinit var syncMetadataUseCase: br.acerola.comic.usecase.sync.SyncMetadataUseCase

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

    private fun buildWorker(inputData: Data): MetadataSyncWorker {
        val notificationHelper = NotificationHelper(context)
        return TestListenableWorkerBuilder<MetadataSyncWorker>(context)
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
                        syncMetadataUseCase = syncMetadataUseCase,
                        anilistSyncUseCase = anilistSyncUseCase,
                        mangadexSyncUseCase = mangadexSyncUseCase,
                        comicInfoSyncUseCase = comicInfoSyncUseCase,
                        notificationHelper = notificationHelper,
                    )
                },
            ).build()
    }

    // Teste para directoryId != -1L (sincronização de comic individual)
    @Test
    fun `doWork should return success when single comic synchronization succeeds`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    MetadataSource.MANGADEX,
                    SyncType.SYNC,
                    42L,
                )
            } returns Either.Right(Unit)

            val worker =
                buildWorker(
                    workDataOf(
                        MetadataSyncWorker.KEY_SYNC_SOURCE to MetadataSyncWorker.SOURCE_MANGADEX,
                        MetadataSyncWorker.KEY_DIRECTORY_ID to 42L,
                    ),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Success)
            coVerify(exactly = 1) {
                syncMetadataUseCase.execute(
                    MetadataSource.MANGADEX,
                    SyncType.SYNC,
                    42L,
                )
            }
        }

    @Test
    fun `doWork should return failure when single comic synchronization fails`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    MetadataSource.MANGADEX,
                    SyncType.SYNC,
                    42L,
                )
            } returns Either.Left(LibrarySyncError.SyncNetworkError())

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
    fun `doWork should return success when single comic synchronization with Anilist succeeds`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    MetadataSource.ANILIST,
                    SyncType.SYNC,
                    42L,
                )
            } returns Either.Right(Unit)

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
    fun `doWork should return success when single comic synchronization with ComicInfo succeeds`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    MetadataSource.COMIC_INFO,
                    SyncType.SYNC,
                    42L,
                )
            } returns Either.Right(Unit)

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

    // Teste para sincronização de toda a biblioteca
    @Test
    fun `doWork should return success when Mangadex library synchronization succeeds`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    MetadataSource.MANGADEX,
                    SyncType.SYNC,
                    null,
                )
            } returns Either.Right(Unit)

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
    fun `doWork should return failure when Mangadex library synchronization fails`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    MetadataSource.MANGADEX,
                    SyncType.SYNC,
                    null,
                )
            } returns Either.Left(LibrarySyncError.SyncNetworkError())

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
    fun `doWork should return success when Anilist library rescan succeeds`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    MetadataSource.ANILIST,
                    SyncType.RESCAN,
                    null,
                )
            } returns Either.Right(Unit)

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
    fun `doWork should return success when ComicInfo library synchronization succeeds`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    MetadataSource.COMIC_INFO,
                    SyncType.SYNC,
                    null,
                )
            } returns Either.Right(Unit)

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
        runTest {
            coEvery {
                syncMetadataUseCase.execute(any(), any(), any())
            } throws RuntimeException("Fatal error")

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
