export type ComicSummaryRelationsPayload = {
	directoryId: string;
	metadataId: string | null;
};

export type ComicSummaryFilesystemPayload = {
	folderName: string;
};

export type ComicSummaryMetadataPayload = {
	title: string | null;
	externalSync: boolean;
	activeSource: string | null;
	chapterCount: number;
	description?: string | null;
	status?: string | null;
	author?: string | null;
};

export type ComicSummaryArtworkPayload = {
	cover: string | null;
	banner: string | null;
};

export type ComicSummaryBookmarkPayload = {
	id: number;
	name: string;
	color: number;
};

export type ComicSummaryItemPayload = {
	relations: ComicSummaryRelationsPayload;
	filesystem: ComicSummaryFilesystemPayload;
	metadata: ComicSummaryMetadataPayload;
	artwork: ComicSummaryArtworkPayload;
	bookmark?: ComicSummaryBookmarkPayload | null;
};

export type ComicSummaryPayload = {
	comics: ComicSummaryItemPayload[];
	total: number;
	fetchedAt: string;
};

export type SortBy = 'title' | 'chapterCount' | 'lastUpdated';
export type SortOrder = 'asc' | 'desc';
export type MetadataSource = 'all' | 'comicinfo' | 'mangadex' | 'anilist' | 'no_metadata';

export type SortConfig = {
	sortBy: SortBy;
	sortOrder: SortOrder;
};

export type FilterConfig = {
	showHidden: boolean;
	metadataSource: MetadataSource;
};
