import { invoke } from '@tauri-apps/api/core';
import { error } from '@tauri-apps/plugin-log';

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

/**
 * Hook to manage categories (bookmarks) in the global state.
 * It provides methods to fetch, create, and delete bookmarks,
 * as well as assigning/removing bookmarks from a specific comic.
 *
 * @returns An object containing the bookmarks state and mutation methods.
 */
export function useBookmarks() {
    let bookmarks = $state<Category[]>([]);
    let isLoading = $state(false);

    async function loadBookmarks() {
        isLoading = true;
        try {
            bookmarks = await invoke<Category[]>('get_categories');
        } catch (err) {
            error(`Failed to load bookmarks: ${err}`);
        } finally {
            isLoading = false;
        }
    }

    async function createBookmark(name: string, color: number) {
        try {
            const newBookmark = await invoke<Category>('create_category', { name, color });
            bookmarks = [...bookmarks, newBookmark];
            return newBookmark;
        } catch (err) {
            error(`Failed to create bookmark: ${err}`);
            throw err;
        }
    }

    async function deleteBookmark(id: number) {
        try {
            await invoke('delete_category', { id });
            bookmarks = bookmarks.filter((bookmark) => bookmark.id !== id);
        } catch (err) {
            error(`Failed to delete bookmark: ${err}`);
            throw err;
        }
    }

    async function assignToComic(comicId: number, categoryId: number) {
        try {
            // Como a tabela tem `comic_directory_fk` definido como UNIQUE,
            // cada quadrinho pode ter no máximo um marcador associado.
            // Garantimos a exclusão do vínculo anterior antes de criar o novo.
            await invoke('remove_category_from_comic', { comicId });
            return await invoke<MangaCategory>('assign_category_to_comic', { comicId, categoryId });
        } catch (err) {
            error(`Failed to assign bookmark: ${err}`);
            throw err;
        }
    }

    async function removeComicBookmark(comicId: number) {
        try {
            await invoke('remove_category_from_comic', { comicId });
        } catch (err) {
            error(`Failed to remove bookmark from comic: ${err}`);
            throw err;
        }
    }

    async function getComicBookmark(comicId: number) {
        try {
            return await invoke<Category | null>('get_comic_category', { comicId });
        } catch (err) {
            error(`Failed to get comic bookmark: ${err}`);
            return null;
        }
    }

    return {
        get bookmarks() { return bookmarks; },
        get isLoading() { return isLoading; },
        loadBookmarks,
        createBookmark,
        deleteBookmark,
        assignToComic,
        removeComicBookmark,
        getComicBookmark,
    };
}
