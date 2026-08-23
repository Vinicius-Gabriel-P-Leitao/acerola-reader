package br.acerola.comic.sync

import androidx.work.WorkInfo
import androidx.work.WorkManager
import br.acerola.comic.di.ApplicationScope
import br.acerola.comic.worker.contract.WorkerContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerLibrarySyncStatusRepository
    @Inject
    constructor(
        workManager: WorkManager,
        @ApplicationScope scope: CoroutineScope,
    ) : LibrarySyncStatusRepository {
        override val isIndexing: StateFlow<Boolean> =
            workManager
                .getWorkInfosByTagFlow(WorkerContract.TAG_LIBRARY_SYNC)
                .map { workInfos ->
                    workInfos.any { !it.state.isFinished }
                }.stateIn(
                    scope = scope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                    initialValue = false,
                )

        override val progress: StateFlow<Int> =
            workManager
                .getWorkInfosByTagFlow(WorkerContract.TAG_LIBRARY_SYNC)
                .map { workInfos ->
                    val activeWorker = workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING }
                    activeWorker?.progress?.getInt(WorkerContract.KEY_PROGRESS, -1) ?: -1
                }.stateIn(
                    scope = scope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                    initialValue = -1,
                )
    }
