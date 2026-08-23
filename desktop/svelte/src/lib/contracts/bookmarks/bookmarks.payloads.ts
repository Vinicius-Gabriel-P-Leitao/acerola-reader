export type Category = {
	id: number;
	name: string;
	color: number;
};

export type MangaCategory = {
	id: number;
	comic_directory_fk: string;
	category_id: number;
};
