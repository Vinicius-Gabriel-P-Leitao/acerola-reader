package br.acerola.manga.module.manga.state

import br.acerola.manga.config.preference.ChapterPageSizeType
import br.acerola.manga.dto.archive.ChapterFileDto

sealed interface MangaAction {
    data object NavigateBack : MangaAction
    data class SelectTab(val tab: MainTab) : MangaAction
    data class UpdatePageSize(val size: ChapterPageSizeType) : MangaAction
    data class UpdateCategory(val categoryId: Long?) : MangaAction
    data class ToggleExternalSync(val enabled: Boolean) : MangaAction
}

sealed interface MangaChapterAction {
    data class ChangePage(val page: Int) : MangaChapterAction
    data class ClickChapter(val chapter: ChapterFileDto, val initialPage: Int) : MangaChapterAction
    data class ClickContinue(val chapterId: Long, val lastPage: Int) : MangaChapterAction
    data class ToggleReadStatus(val chapterId: Long) : MangaChapterAction
}

sealed interface MangaSyncAction {
    data object SyncChaptersLocal : MangaSyncAction
    data object RescanManga : MangaSyncAction
    data object SyncMangadexInfo : MangaSyncAction
    data object SyncMangadexChapters : MangaSyncAction
    data object SyncComicInfo : MangaSyncAction
    data object SyncComicInfoChapters : MangaSyncAction
    data object SyncAnilistInfo : MangaSyncAction
    data object ExtractFirstPageAsCover : MangaSyncAction
}
