import type { ChapterFileDto } from '$lib/contracts/library/chapter.payloads';

export type ReaderChapterPayload = ChapterFileDto;

export type ReaderSessionPayload = {
	chapter: ReaderChapterPayload;
	pageCount: number;
	currentPage: number;
	cacheCapacity: number;
};

export type ReaderPagePayload = {
	chapterId: string;
	index: number;
	total: number;
	mimeType: string;
	bytes: number[];
	cacheHit: boolean;
};

export type ReaderCachedPage = {
	index: number;
	total: number;
	mimeType: string;
	url: string;
	cacheHit: boolean;
};

export type ReaderStatusPayload = {
	isOpen: boolean;
	chapterId: string | null;
	pageCount: number;
	currentPage: number | null;
	cacheKeys: number[];
	cacheCapacity: number;
};
