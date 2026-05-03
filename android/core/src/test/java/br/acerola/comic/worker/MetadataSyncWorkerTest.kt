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
import br.acerola.comic.util.notification.NotificationHelper
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

    // Test for directoryId != -1L (Single comic sync)
    @Test
    fun `doWork deve retornar sucesso quando sincronizacao para um unico comic tem sucesso`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    br.acerola.comic.pattern.metadata.MetadataSource.MANGADEX,
                    br.acerola.comic.worker.contract.SyncType.SYNC,
                    42L
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
                    br.acerola.comic.pattern.metadata.MetadataSource.MANGADEX,
                    br.acerola.comic.worker.contract.SyncType.SYNC,
                    42L
                )
            }
        }

    @Test
    fun `doWork deve retornar falha quando sincronizacao para um unico comic falha`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    br.acerola.comic.pattern.metadata.MetadataSource.MANGADEX,
                    br.acerola.comic.worker.contract.SyncType.SYNC,
                    42L
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
    fun `doWork deve retornar sucesso quando sincronizacao para um unico comic com Anilist tem sucesso`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    br.acerola.comic.pattern.metadata.MetadataSource.ANILIST,
                    br.acerola.comic.worker.contract.SyncType.SYNC,
                    42L
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
    fun `doWork deve retornar sucesso quando sincronizacao para um unico comic com ComicInfo tem sucesso`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    br.acerola.comic.pattern.metadata.MetadataSource.COMIC_INFO,
                    br.acerola.comic.worker.contract.SyncType.SYNC,
                    42L
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

    // Test for Library-wide sync
    @Test
    fun `doWork deve retornar sucesso quando sincronizacao da biblioteca Mangadex tem sucesso`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    br.acerola.comic.pattern.metadata.MetadataSource.MANGADEX,
                    br.acerola.comic.worker.contract.SyncType.SYNC,
                    null
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
    fun `doWork deve retornar falha quando sincronizacao da biblioteca Mangadex falha`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    br.acerola.comic.pattern.metadata.MetadataSource.MANGADEX,
                    br.acerola.comic.worker.contract.SyncType.SYNC,
                    null
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
    fun `doWork deve retornar sucesso quando rescan da biblioteca Anilist tem sucesso`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    br.acerola.comic.pattern.metadata.MetadataSource.ANILIST,
                    br.acerola.comic.worker.contract.SyncType.RESCAN,
                    null
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
    fun `doWork deve retornar sucesso quando sincronizacao da biblioteca ComicInfo tem sucesso`() =
        runTest {
            coEvery {
                syncMetadataUseCase.execute(
                    br.acerola.comic.pattern.metadata.MetadataSource.COMIC_INFO,
                    br.acerola.comic.worker.contract.SyncType.SYNC,
                    null
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
    fun `doWork deve lidar com excecoes graciosamente e retornar falha`() =
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
