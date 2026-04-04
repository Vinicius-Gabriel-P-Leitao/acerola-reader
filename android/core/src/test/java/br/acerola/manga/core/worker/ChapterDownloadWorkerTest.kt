package br.acerola.manga.core.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import br.acerola.manga.core.usecase.download.DownloadChaptersUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChapterDownloadWorkerTest {

    private lateinit var context: Context

    @MockK
    lateinit var workManager: WorkManager

    @MockK
    lateinit var downloadChaptersUseCase: DownloadChaptersUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()
    }

    private fun buildWorker(inputData: Data): ChapterDownloadWorker =
        TestListenableWorkerBuilder<ChapterDownloadWorker>(context)
            .setInputData(inputData)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ) = ChapterDownloadWorker(
                    workManager,
                    appContext,
                    workerParameters,
                    downloadChaptersUseCase
                )
            })
            .build()

    @Test
    fun `doWork should return failure when chapter_ids is missing`() = runBlocking {
        val worker = buildWorker(
            workDataOf(
                ChapterDownloadWorker.KEY_MANGA_TITLE to "Naruto",
                ChapterDownloadWorker.KEY_BASE_URI to "content://mock/tree"
            )
        )

        val result = worker.doWork()

        assertTrue(result is Result.Failure)
    }

    @Test
    fun `doWork should return failure when manga_title is missing`() = runBlocking {
        val worker = buildWorker(
            workDataOf(
                ChapterDownloadWorker.KEY_CHAPTER_IDS to arrayOf("1", "2"),
                ChapterDownloadWorker.KEY_BASE_URI to "content://mock/tree"
            )
        )

        val result = worker.doWork()

        assertTrue(result is Result.Failure)
    }

    @Test
    fun `doWork should return failure when base_uri is missing`() = runBlocking {
        val worker = buildWorker(
            workDataOf(
                ChapterDownloadWorker.KEY_CHAPTER_IDS to arrayOf("1", "2"),
                ChapterDownloadWorker.KEY_MANGA_TITLE to "Naruto"
            )
        )

        val result = worker.doWork()

        assertTrue(result is Result.Failure)
    }

    // Since DocumentFile.fromTreeUri and other android specific file APIs might fail in Robolectric without proper shadow or setup, 
    // the base_uri parsing might fail with "Cannot access library folder" and return Result.Failure.
    @Test
    fun `doWork should return failure when library folder cannot be accessed`() = runBlocking {
        io.mockk.mockkStatic(androidx.documentfile.provider.DocumentFile::class)
        io.mockk.every { androidx.documentfile.provider.DocumentFile.fromTreeUri(any(), any()) } returns null
        val worker = buildWorker(
            workDataOf(
                ChapterDownloadWorker.KEY_CHAPTER_IDS to arrayOf("1", "2"),
                ChapterDownloadWorker.KEY_MANGA_TITLE to "Naruto",
                ChapterDownloadWorker.KEY_BASE_URI to "invalid_uri"
            )
        )

        val result = worker.doWork()

        assertTrue(result is Result.Failure)
        io.mockk.unmockkStatic(androidx.documentfile.provider.DocumentFile::class)
    }

    @Test
    fun `doWork should execute download and return success when downloads finish successfully`() = runBlocking {
        // We cannot easily mock DocumentFile.fromTreeUri since it's a static method without mockkStatic
        // We'll test with mockkStatic to mock DocumentFile behavior
        io.mockk.mockkStatic(androidx.documentfile.provider.DocumentFile::class)
        val mockLibraryRoot = io.mockk.mockk<androidx.documentfile.provider.DocumentFile>()
        val mockMangaFolder = io.mockk.mockk<androidx.documentfile.provider.DocumentFile>()
        
        io.mockk.every { androidx.documentfile.provider.DocumentFile.fromTreeUri(any(), any()) } returns mockLibraryRoot
        io.mockk.every { mockLibraryRoot.findFile("Naruto") } returns mockMangaFolder

        coEvery { downloadChaptersUseCase(any(), any(), any(), any(), any()) } returns DownloadChaptersUseCase.Result(
            downloadedCount = 2,
            errorCount = 0
        )

        val mockOperation = io.mockk.mockk<androidx.work.Operation>()
        io.mockk.every { workManager.beginWith(any<androidx.work.OneTimeWorkRequest>()).then(any<androidx.work.OneTimeWorkRequest>()).enqueue() } returns mockOperation

        val worker = buildWorker(
            workDataOf(
                ChapterDownloadWorker.KEY_CHAPTER_IDS to arrayOf("1", "2"),
                ChapterDownloadWorker.KEY_CHAPTER_NUMBERS to arrayOf("1", "2"),
                ChapterDownloadWorker.KEY_MANGA_TITLE to "Naruto",
                ChapterDownloadWorker.KEY_BASE_URI to "content://mock/tree"
            )
        )

        val result = worker.doWork()

        assertTrue(result is Result.Success)
        io.mockk.unmockkStatic(androidx.documentfile.provider.DocumentFile::class)
    }

    @Test
    fun `doWork should return failure when all downloads fail`() = runBlocking {
        io.mockk.mockkStatic(androidx.documentfile.provider.DocumentFile::class)
        val mockLibraryRoot = io.mockk.mockk<androidx.documentfile.provider.DocumentFile>()
        val mockMangaFolder = io.mockk.mockk<androidx.documentfile.provider.DocumentFile>()
        
        io.mockk.every { androidx.documentfile.provider.DocumentFile.fromTreeUri(any(), any()) } returns mockLibraryRoot
        io.mockk.every { mockLibraryRoot.findFile("Naruto") } returns mockMangaFolder

        coEvery { downloadChaptersUseCase(any(), any(), any(), any(), any()) } returns DownloadChaptersUseCase.Result(
            downloadedCount = 0,
            errorCount = 2
        )

        val mockOperation = io.mockk.mockk<androidx.work.Operation>()
        io.mockk.every { workManager.beginWith(any<androidx.work.OneTimeWorkRequest>()).then(any<androidx.work.OneTimeWorkRequest>()).enqueue() } returns mockOperation

        val worker = buildWorker(
            workDataOf(
                ChapterDownloadWorker.KEY_CHAPTER_IDS to arrayOf("1", "2"),
                ChapterDownloadWorker.KEY_CHAPTER_NUMBERS to arrayOf("1", "2"),
                ChapterDownloadWorker.KEY_MANGA_TITLE to "Naruto",
                ChapterDownloadWorker.KEY_BASE_URI to "content://mock/tree"
            )
        )

        val result = worker.doWork()

        assertTrue(result is Result.Failure)
        io.mockk.unmockkStatic(androidx.documentfile.provider.DocumentFile::class)
    }
}
