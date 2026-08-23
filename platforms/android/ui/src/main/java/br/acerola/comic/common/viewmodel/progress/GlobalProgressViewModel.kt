package br.acerola.comic.common.viewmodel.progress
import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import br.acerola.comic.worker.contract.WorkerContract
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class GlobalProgressViewModel
    @Inject
    constructor(
        workManager: WorkManager,
    ) : ViewModel() {
        private val librarySyncInfos: Flow<List<WorkInfo>> =
            workManager.getWorkInfosByTagFlow(WorkerContract.TAG_LIBRARY_SYNC)

        private val metadataSyncInfos: Flow<List<WorkInfo>> =
            workManager.getWorkInfosByTagFlow(WorkerContract.TAG_METADATA_SYNC)

        val isIndexing: Flow<Boolean> =
            combine(librarySyncInfos, metadataSyncInfos) { library, metadata ->
                (library + metadata).any { !it.state.isFinished }
            }

        val progress: Flow<Float?> =
            combine(librarySyncInfos, metadataSyncInfos) { library, metadata ->
                val runningWork = (library + metadata).firstOrNull { it.state == WorkInfo.State.RUNNING }
                val rawProgress = runningWork?.progress?.getInt(WorkerContract.KEY_PROGRESS, -1) ?: -1
                if (rawProgress >= 0) rawProgress / 100f else null
            }
    }
