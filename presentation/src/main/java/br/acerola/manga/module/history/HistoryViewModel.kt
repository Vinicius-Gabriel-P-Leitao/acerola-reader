package br.acerola.manga.module.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.acerola.manga.data.repository.HistoryRepository
import br.acerola.manga.dto.MangaDto
import br.acerola.manga.dto.archive.MangaDirectoryDto
import br.acerola.manga.dto.metadata.manga.MangaRemoteInfoDto
import br.acerola.manga.usecase.di.DirectoryCase
import br.acerola.manga.usecase.di.MangadexCase
import br.acerola.manga.usecase.manga.ObserveLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    @param:MangadexCase private val mangadexObserve: ObserveLibraryUseCase<MangaRemoteInfoDto>,
    @param:DirectoryCase private val directoryObserve: ObserveLibraryUseCase<MangaDirectoryDto>,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyItems: StateFlow<List<MangaDto>> = historyRepository.getAllRecentHistory()
        .flatMapLatest { historyList ->
            combine(
                directoryObserve(),
                mangadexObserve()
            ) { directories, remoteInfos ->
                historyList.mapNotNull { history ->
                    val directory = directories.find { it.id == history.mangaId } ?: return@mapNotNull null
                    val remote = remoteInfos.find { it.mangaDirectoryFk == history.mangaId }
                    MangaDto(directory = directory, remoteInfo = remote)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
