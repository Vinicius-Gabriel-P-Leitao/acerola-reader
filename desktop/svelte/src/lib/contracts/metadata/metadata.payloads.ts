export type ComicMetadataEvent = {
	id: string;
	title: string;
	description: string;
	status: string;
	publication: number | null;
	syncSource: string | null;
	hasComicInfo: boolean;
	comicDirectoryFk: string | null;
};
