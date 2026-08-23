package br.acerola.comic.common.viewmodel.progress

import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.acerola.comic.worker.contract.WorkerContract
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalProgressViewModelTest {
    private val workManager = mockk<WorkManager>()

    private fun workInfo(
        state: WorkInfo.State,
        progress: Data = Data.EMPTY,
    ) = WorkInfo(
        UUID.randomUUID(),
        state,
        emptySet(),
        Data.EMPTY,
        progress,
        0,
    )

    private fun viewModel(
        library: List<WorkInfo> = emptyList(),
        metadata: List<WorkInfo> = emptyList(),
    ): GlobalProgressViewModel {
        every { workManager.getWorkInfosByTagFlow(WorkerContract.TAG_LIBRARY_SYNC) } returns flowOf(library)
        every { workManager.getWorkInfosByTagFlow(WorkerContract.TAG_METADATA_SYNC) } returns flowOf(metadata)
        return GlobalProgressViewModel(workManager)
    }

    // Testes de isIndexing

    @Test
    fun `isIndexing should be true when library sync is RUNNING`() =
        runTest {
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.RUNNING)))

            assertTrue(vm.isIndexing.first())
        }

    @Test
    fun `isIndexing should be true when metadata sync is RUNNING`() =
        runTest {
            val vm = viewModel(metadata = listOf(workInfo(WorkInfo.State.RUNNING)))

            assertTrue(vm.isIndexing.first())
        }

    @Test
    fun `isIndexing should be true when work is ENQUEUED`() =
        runTest {
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.ENQUEUED)))

            assertTrue(vm.isIndexing.first())
        }

    @Test
    fun `isIndexing should be true when work is BLOCKED`() =
        runTest {
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.BLOCKED)))

            assertTrue(vm.isIndexing.first())
        }

    @Test
    fun `isIndexing should be false when both lists are empty`() =
        runTest {
            val vm = viewModel()

            assertFalse(vm.isIndexing.first())
        }

    @Test
    fun `isIndexing should be false when all jobs are completed with SUCCEEDED`() =
        runTest {
            val vm =
                viewModel(
                    library = listOf(workInfo(WorkInfo.State.SUCCEEDED)),
                    metadata = listOf(workInfo(WorkInfo.State.SUCCEEDED)),
                )

            assertFalse(vm.isIndexing.first())
        }

    @Test
    fun `isIndexing should be false when jobs are FAILED or CANCELLED`() =
        runTest {
            val vm =
                viewModel(
                    library = listOf(workInfo(WorkInfo.State.FAILED)),
                    metadata = listOf(workInfo(WorkInfo.State.CANCELLED)),
                )

            assertFalse(vm.isIndexing.first())
        }

    @Test
    fun `isIndexing should be true when at least one job is not finished among multiple`() =
        runTest {
            val vm =
                viewModel(
                    library = listOf(workInfo(WorkInfo.State.SUCCEEDED), workInfo(WorkInfo.State.RUNNING)),
                )

            assertTrue(vm.isIndexing.first())
        }

    // Testes de progress

    @Test
    fun `progress should return null when there is no RUNNING work`() =
        runTest {
            val vm = viewModel()

            assertNull(vm.progress.first())
        }

    @Test
    fun `progress should return null when work is ENQUEUED without progress`() =
        runTest {
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.ENQUEUED)))

            assertNull(vm.progress.first())
        }

    @Test
    fun `progress should return null when work is RUNNING but progress is -1`() =
        runTest {
            val progressData = workDataOf(WorkerContract.KEY_PROGRESS to -1)
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.RUNNING, progressData)))

            assertNull(vm.progress.first())
        }

    @Test
    fun `progress should return null when work is RUNNING without progress key`() =
        runTest {
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.RUNNING, Data.EMPTY)))

            assertNull(vm.progress.first())
        }

    @Test
    fun `progress should return 0_5f when progress is 50`() =
        runTest {
            val progressData = workDataOf(WorkerContract.KEY_PROGRESS to 50)
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.RUNNING, progressData)))

            assertEquals(0.5f, vm.progress.first())
        }

    @Test
    fun `progress should return 1_0f when progress is 100`() =
        runTest {
            val progressData = workDataOf(WorkerContract.KEY_PROGRESS to 100)
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.RUNNING, progressData)))

            assertEquals(1.0f, vm.progress.first())
        }

    @Test
    fun `progress should return 0_0f when progress is 0`() =
        runTest {
            val progressData = workDataOf(WorkerContract.KEY_PROGRESS to 0)
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.RUNNING, progressData)))

            assertEquals(0.0f, vm.progress.first())
        }

    @Test
    fun `progress should use metadata sync when library sync is not RUNNING`() =
        runTest {
            val progressData = workDataOf(WorkerContract.KEY_PROGRESS to 75)
            val vm =
                viewModel(
                    library = listOf(workInfo(WorkInfo.State.SUCCEEDED)),
                    metadata = listOf(workInfo(WorkInfo.State.RUNNING, progressData)),
                )

            assertEquals(0.75f, vm.progress.first())
        }

    @Test
    fun `progress should prioritize the first RUNNING work found`() =
        runTest {
            val libraryProgress = workDataOf(WorkerContract.KEY_PROGRESS to 30)
            val metadataProgress = workDataOf(WorkerContract.KEY_PROGRESS to 80)
            val vm =
                viewModel(
                    library = listOf(workInfo(WorkInfo.State.RUNNING, libraryProgress)),
                    metadata = listOf(workInfo(WorkInfo.State.RUNNING, metadataProgress)),
                )

            assertEquals(0.3f, vm.progress.first())
        }
}
