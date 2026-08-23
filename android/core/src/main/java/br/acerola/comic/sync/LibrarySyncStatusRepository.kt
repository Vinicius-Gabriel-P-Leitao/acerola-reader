package br.acerola.comic.sync

import kotlinx.coroutines.flow.StateFlow

interface LibrarySyncStatusRepository {
    val isIndexing: StateFlow<Boolean>
    val progress: StateFlow<Int>
}
