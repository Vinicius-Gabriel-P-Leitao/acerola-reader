export const HISTORY_COMMANDS = {
	getAll: 'history_get_all',
	getComic: 'history_get_comic',
	getReadChapters: 'history_get_read_chapters',
	updateReading: 'history_update_reading',
	clear: 'history_clear',
	markChapterRead: 'history_mark_chapter_read',
	unmarkChapterRead: 'history_unmark_chapter_read',
	markChaptersReadBatch: 'history_mark_chapters_read_batch',
	unmarkChaptersReadBatch: 'history_unmark_chapters_read_batch'
} as const;
