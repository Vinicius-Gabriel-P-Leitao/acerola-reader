package br.acerola.manga.common.viewmodel.progress

import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject


// FIXME: Isso não tá frágil de mais? tem que ter um contrato defindo os valores
@HiltViewModel
class GlobalProgressViewModel @Inject constructor(
    workManager: WorkManager
) : ViewModel() {

    private val librarySyncInfos: Flow<List<WorkInfo>> =
        workManager.getWorkInfosByTagFlow("library_sync")

    private val metadataSyncInfos: Flow<List<WorkInfo>> =
        workManager.getWorkInfosByTagFlow("metadata_sync")

    val isIndexing: Flow<Boolean> = combine(librarySyncInfos, metadataSyncInfos) { library, metadata ->
        (library + metadata).any { !it.state.isFinished }
    }

    val progress: Flow<Float?> = combine(librarySyncInfos, metadataSyncInfos) { library, metadata ->
        val runningWork = (library + metadata).firstOrNull { it.state == WorkInfo.State.RUNNING }
        val rawProgress = runningWork?.progress?.getInt("progress", -1) ?: -1
        if (rawProgress >= 0) rawProgress / 100f else null
    }
}
