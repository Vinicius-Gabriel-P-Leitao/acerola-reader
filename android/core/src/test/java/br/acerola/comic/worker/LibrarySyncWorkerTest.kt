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
import br.acerola.comic.adapter.contract.gateway.ComicGateway
import br.acerola.comic.dto.archive.ComicDirectoryDto
import br.acerola.comic.error.message.LibrarySyncError
import br.acerola.comic.util.notification.NotificationHelper
import br.acerola.comic.worker.sync.LibrarySyncWorker
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
class LibrarySyncWorkerTest {
    private lateinit var context: Context

    @MockK
    lateinit var syncLibraryUseCase: br.acerola.comic.usecase.sync.SyncLibraryUseCase

    @MockK
    lateinit var repository: ComicGateway<ComicDirectoryDto>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()
        every { repository.progress } returns MutableStateFlow(0)
        every { repository.isIndexing } returns MutableStateFlow(false)
    }

    private fun buildWorker(inputData: Data): LibrarySyncWorker {
        val notificationHelper = NotificationHelper(context)
        return TestListenableWorkerBuilder<LibrarySyncWorker>(context)
            .setInputData(inputData)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ) = LibrarySyncWorker(
                        appContext,
                        workerParameters,
                        syncLibraryUseCase = syncLibraryUseCase,
                        progressGateway = repository,
                        notificationHelper = notificationHelper,
                    )
                },
            ).build()
    }

    // SYNC_TYPE_SPECIFIC

    @Test
    fun `doWork deve retornar falha quando comicId é -1 em SYNC_TYPE_SPECIFIC`() =
        runTest {
            coEvery {
                syncLibraryUseCase.execute(
                    br.acerola.comic.worker.contract.SyncType.SPECIFIC,
                    -1L,
                    null
                )
            } returns Either.Left(LibrarySyncError.UnexpectedError(Exception("Comic ID not found")))

            val worker =
                buildWorker(
                    workDataOf(
                        LibrarySyncWorker.KEY_SYNC_TYPE to LibrarySyncWorker.SYNC_TYPE_SPECIFIC,
                        LibrarySyncWorker.KEY_MANGA_ID to -1L,
                    ),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Failure)
        }

    @Test
    fun `doWork deve retornar sucesso quando SYNC_TYPE_SPECIFIC com comicId válido`() =
        runTest {
            coEvery {
                syncLibraryUseCase.execute(
                    br.acerola.comic.worker.contract.SyncType.SPECIFIC,
                    42L,
                    null
                )
            } returns Either.Right(Unit)

            val worker =
                buildWorker(
                    workDataOf(
                        LibrarySyncWorker.KEY_SYNC_TYPE to LibrarySyncWorker.SYNC_TYPE_SPECIFIC,
                        LibrarySyncWorker.KEY_MANGA_ID to 42L,
                    ),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Success)
            coVerify(exactly = 1) {
                syncLibraryUseCase.execute(
                    br.acerola.comic.worker.contract.SyncType.SPECIFIC,
                    42L,
                    null
                )
            }
        }

    @Test
    fun `doWork deve retornar falha quando repositório falha em SYNC_TYPE_SPECIFIC`() =
        runTest {
            coEvery {
                syncLibraryUseCase.execute(
                    br.acerola.comic.worker.contract.SyncType.SPECIFIC,
                    42L,
                    null
                )
            } returns Either.Left(LibrarySyncError.DatabaseError())

            val worker =
                buildWorker(
                    workDataOf(
                        LibrarySyncWorker.KEY_SYNC_TYPE to LibrarySyncWorker.SYNC_TYPE_SPECIFIC,
                        LibrarySyncWorker.KEY_MANGA_ID to 42L,
                    ),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Failure)
        }

    // SYNC_TYPE_INCREMENTAL

    @Test
    fun `doWork deve retornar sucesso em SYNC_TYPE_INCREMENTAL`() =
        runTest {
            coEvery {
                syncLibraryUseCase.execute(
                    br.acerola.comic.worker.contract.SyncType.INCREMENTAL,
                    -1L,
                    null
                )
            } returns Either.Right(Unit)

            val worker =
                buildWorker(
                    workDataOf(LibrarySyncWorker.KEY_SYNC_TYPE to LibrarySyncWorker.SYNC_TYPE_INCREMENTAL),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Success)
        }

    @Test
    fun `doWork deve retornar falha quando repositório falha em SYNC_TYPE_INCREMENTAL`() =
        runTest {
            coEvery {
                syncLibraryUseCase.execute(
                    br.acerola.comic.worker.contract.SyncType.INCREMENTAL,
                    -1L,
                    null
                )
            } returns Either.Left(LibrarySyncError.FolderAccessDenied())

            val worker =
                buildWorker(
                    workDataOf(LibrarySyncWorker.KEY_SYNC_TYPE to LibrarySyncWorker.SYNC_TYPE_INCREMENTAL),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Failure)
        }

    // SYNC_TYPE_REFRESH

    @Test
    fun `doWork deve retornar sucesso em SYNC_TYPE_REFRESH`() =
        runTest {
            coEvery {
                syncLibraryUseCase.execute(
                    br.acerola.comic.worker.contract.SyncType.REFRESH,
                    -1L,
                    null
                )
            } returns Either.Right(Unit)

            val worker =
                buildWorker(
                    workDataOf(LibrarySyncWorker.KEY_SYNC_TYPE to LibrarySyncWorker.SYNC_TYPE_REFRESH),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Success)
        }

    @Test
    fun `doWork deve retornar falha quando repositório falha em SYNC_TYPE_REFRESH`() =
        runTest {
            coEvery {
                syncLibraryUseCase.execute(
                    br.acerola.comic.worker.contract.SyncType.REFRESH,
                    -1L,
                    null
                )
            } returns Either.Left(LibrarySyncError.SyncNetworkError())

            val worker =
                buildWorker(
                    workDataOf(LibrarySyncWorker.KEY_SYNC_TYPE to LibrarySyncWorker.SYNC_TYPE_REFRESH),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Failure)
        }

    // SYNC_TYPE_REBUILD

    @Test
    fun `doWork deve retornar sucesso em SYNC_TYPE_REBUILD`() =
        runTest {
            coEvery {
                syncLibraryUseCase.execute(
                    br.acerola.comic.worker.contract.SyncType.REBUILD,
                    -1L,
                    null
                )
            } returns Either.Right(Unit)

            val worker =
                buildWorker(
                    workDataOf(LibrarySyncWorker.KEY_SYNC_TYPE to LibrarySyncWorker.SYNC_TYPE_REBUILD),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Success)
        }

    @Test
    fun `doWork deve retornar falha quando repositório falha em SYNC_TYPE_REBUILD`() =
        runTest {
            coEvery {
                syncLibraryUseCase.execute(
                    br.acerola.comic.worker.contract.SyncType.REBUILD,
                    -1L,
                    null
                )
            } returns Either.Left(LibrarySyncError.MalformedLibrary())

            val worker =
                buildWorker(
                    workDataOf(LibrarySyncWorker.KEY_SYNC_TYPE to LibrarySyncWorker.SYNC_TYPE_REBUILD),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Failure)
        }

    // Fallback / default sync type

    @Test
    fun `doWork deve usar incrementalScan como fallback para tipo desconhecido`() =
        runTest {
            coEvery {
                syncLibraryUseCase.execute(
                    br.acerola.comic.worker.contract.SyncType.INCREMENTAL,
                    -1L,
                    null
                )
            } returns Either.Right(Unit)

            val worker =
                buildWorker(
                    workDataOf(LibrarySyncWorker.KEY_SYNC_TYPE to "unknown_type"),
                )

            val result = worker.doWork()

            assertTrue(result is Result.Success)
        }
}
