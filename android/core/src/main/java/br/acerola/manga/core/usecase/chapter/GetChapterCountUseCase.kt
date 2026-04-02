package br.acerola.manga.core.usecase.chapter

import br.acerola.manga.adapter.library.ChapterArchiveEngine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChapterCountUseCase @Inject constructor(
    private val chapterArchiveEngine: ChapterArchiveEngine
) {
    operator fun invoke(): Flow<Map<Long, Int>> {
        return chapterArchiveEngine.observeAllChapterCounts()
    }
}
