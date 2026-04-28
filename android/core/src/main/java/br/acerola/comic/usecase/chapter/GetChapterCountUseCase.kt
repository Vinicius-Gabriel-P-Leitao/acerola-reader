package br.acerola.comic.usecase.chapter

import br.acerola.comic.adapter.library.ChapterArchiveEngine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChapterCountUseCase
    @Inject
    constructor(
        private val chapterArchiveEngine: ChapterArchiveEngine,
    ) {
        operator fun invoke(): Flow<Map<Long, Int>> = chapterArchiveEngine.observeAllChapterCounts()
    }
