export type ReadingHistoryPayload = {
	comicDirectoryId: string;
	chapterArchiveId: string;
	lastPage: number;
	isCompleted: boolean;
	updatedAt: number;
	comicName: string;
	comicCover: string | null;
	chapterName: string;
	folderName: string;
	chapterPath: string;
	chapterSort: string;
	isSpecial: boolean;
	lastModified: number;
};
