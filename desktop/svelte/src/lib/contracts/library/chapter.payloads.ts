export enum VolumeViewType {
	VOLUME = 'VOLUME',
	CHAPTER = 'CHAPTER',
	COVER_VOLUME = 'COVER_VOLUME'
}

export type ChapterFilePayload = {
	id: string;
	name: string;
	path: string;
	chapterSort: string;
	volumeId: string | null;
	volumeName: string | null;
	isSpecial: boolean;
	lastModified: number;
};

export type VolumeArchivePayload = {
	id: string;
	name: string;
	volumeSort: string;
	isSpecial: boolean;
	coverUri: string | null;
	bannerUri: string | null;
	lastModified: number;
	chapterCount: number;
};

export type VolumeChapterGroupPayload = {
	volume: VolumeArchivePayload;
	items: ChapterFilePayload[];
	totalChapters: number;
	loadedCount: number;
	hasMore: boolean;
	currentPage: number;
	totalPages: number;
};

export type ChapterPagePayload = {
	items: ChapterFilePayload[];
	volumes: VolumeArchivePayload[];
	pageSize: number;
	page: number;
	total: number;
	volumeSections: VolumeChapterGroupPayload[];
};

export type ChapterPayload = {
	archive: ChapterPagePayload;
	showVolumeHeaders: boolean;
	hasVolumeStructure: boolean;
	effectiveViewMode: VolumeViewType;
};
