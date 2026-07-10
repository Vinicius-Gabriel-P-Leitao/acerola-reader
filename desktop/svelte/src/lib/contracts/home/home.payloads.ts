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
};

export type ComicSummaryArtworkPayload = {
	cover: string | null;
	banner: string | null;
};

export type ComicSummaryItemPayload = {
	relations: ComicSummaryRelationsPayload;
	filesystem: ComicSummaryFilesystemPayload;
	metadata: ComicSummaryMetadataPayload;
	artwork: ComicSummaryArtworkPayload;
};

export type ComicSummaryPayload = {
	comics: ComicSummaryItemPayload[];
	total: number;
	fetchedAt: string;
};

export type SortBy = 'title' | 'chapterCount';
export type SortOrder = 'asc' | 'desc';

export type SortConfig = {
	sortBy: SortBy;
	sortOrder: SortOrder;
};
