export type ComicMetadataEvent = {
	id: string;
	title: string;
	description: string;
	romanji: string;
	status: string;
	publication: number | null;
	syncSource: string | null;
	hasComicInfo: boolean;
	comicDirectoryFk: string | null;
};
