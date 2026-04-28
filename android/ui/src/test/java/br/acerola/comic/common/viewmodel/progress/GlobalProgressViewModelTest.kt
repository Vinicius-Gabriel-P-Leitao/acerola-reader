package br.acerola.comic.common.viewmodel.progress

import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.acerola.comic.worker.WorkerContract
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

    // isIndexing tests

    @Test
    fun `isIndexing deve ser true quando library sync está RUNNING`() =
        runTest {
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.RUNNING)))

            assertTrue(vm.isIndexing.first())
        }

    @Test
    fun `isIndexing deve ser true quando metadata sync está RUNNING`() =
        runTest {
            val vm = viewModel(metadata = listOf(workInfo(WorkInfo.State.RUNNING)))

            assertTrue(vm.isIndexing.first())
        }

    @Test
    fun `isIndexing deve ser true quando trabalho está ENQUEUED`() =
        runTest {
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.ENQUEUED)))

            assertTrue(vm.isIndexing.first())
        }

    @Test
    fun `isIndexing deve ser true quando trabalho está BLOCKED`() =
        runTest {
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.BLOCKED)))

            assertTrue(vm.isIndexing.first())
        }

    @Test
    fun `isIndexing deve ser false quando ambas as listas estão vazias`() =
        runTest {
            val vm = viewModel()

            assertFalse(vm.isIndexing.first())
        }

    @Test
    fun `isIndexing deve ser false quando todos os trabalhos foram concluídos com SUCCEEDED`() =
        runTest {
            val vm =
                viewModel(
                    library = listOf(workInfo(WorkInfo.State.SUCCEEDED)),
                    metadata = listOf(workInfo(WorkInfo.State.SUCCEEDED)),
                )

            assertFalse(vm.isIndexing.first())
        }

    @Test
    fun `isIndexing deve ser false quando trabalhos foram FAILED ou CANCELLED`() =
        runTest {
            val vm =
                viewModel(
                    library = listOf(workInfo(WorkInfo.State.FAILED)),
                    metadata = listOf(workInfo(WorkInfo.State.CANCELLED)),
                )

            assertFalse(vm.isIndexing.first())
        }

    @Test
    fun `isIndexing deve ser true quando ao menos um trabalho não está finalizado entre vários`() =
        runTest {
            val vm =
                viewModel(
                    library = listOf(workInfo(WorkInfo.State.SUCCEEDED), workInfo(WorkInfo.State.RUNNING)),
                )

            assertTrue(vm.isIndexing.first())
        }

    // progress tests

    @Test
    fun `progress deve retornar null quando não há trabalho RUNNING`() =
        runTest {
            val vm = viewModel()

            assertNull(vm.progress.first())
        }

    @Test
    fun `progress deve retornar null quando trabalho está ENQUEUED sem progresso`() =
        runTest {
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.ENQUEUED)))

            assertNull(vm.progress.first())
        }

    @Test
    fun `progress deve retornar null quando trabalho está RUNNING mas progresso é -1`() =
        runTest {
            val progressData = workDataOf(WorkerContract.KEY_PROGRESS to -1)
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.RUNNING, progressData)))

            assertNull(vm.progress.first())
        }

    @Test
    fun `progress deve retornar null quando trabalho está RUNNING sem chave de progresso`() =
        runTest {
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.RUNNING, Data.EMPTY)))

            assertNull(vm.progress.first())
        }

    @Test
    fun `progress deve retornar 0_5f quando progresso é 50`() =
        runTest {
            val progressData = workDataOf(WorkerContract.KEY_PROGRESS to 50)
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.RUNNING, progressData)))

            assertEquals(0.5f, vm.progress.first())
        }

    @Test
    fun `progress deve retornar 1_0f quando progresso é 100`() =
        runTest {
            val progressData = workDataOf(WorkerContract.KEY_PROGRESS to 100)
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.RUNNING, progressData)))

            assertEquals(1.0f, vm.progress.first())
        }

    @Test
    fun `progress deve retornar 0_0f quando progresso é 0`() =
        runTest {
            val progressData = workDataOf(WorkerContract.KEY_PROGRESS to 0)
            val vm = viewModel(library = listOf(workInfo(WorkInfo.State.RUNNING, progressData)))

            assertEquals(0.0f, vm.progress.first())
        }

    @Test
    fun `progress deve usar metadata sync quando library sync não está RUNNING`() =
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
    fun `progress deve priorizar o primeiro trabalho RUNNING encontrado`() =
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
