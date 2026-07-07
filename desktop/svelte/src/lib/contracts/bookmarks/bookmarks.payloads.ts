export type Category = {
    id: number;
    name: string;
    color: number;
};

export type MangaCategory = {
    id: number;
    comic_directory_fk: number;
    category_id: number;
};
