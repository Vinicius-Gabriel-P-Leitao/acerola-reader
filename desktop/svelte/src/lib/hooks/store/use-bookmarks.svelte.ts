import { invoke } from '@tauri-apps/api/core';
import { error } from '@tauri-apps/plugin-log';
import { BOOKMARKS_COMMANDS } from '$lib/contracts/bookmarks/bookmarks.commands';
import type { Category, MangaCategory } from '$lib/contracts/bookmarks/bookmarks.payloads';

let bookmarks = $state<Category[]>([]);
let assignments = $state<MangaCategory[]>([]);
let isLoading = $state(false);
let isInitialized = false;

// ONLY FOR TESTING
export function _resetBookmarksState() {
	bookmarks = [];
	assignments = [];
	isInitialized = false;
	isLoading = false;
}

/**
 * Hook to manage categories (bookmarks) in the global state.
 * It provides methods to fetch, create, and delete bookmarks,
 * as well as assigning/removing bookmarks from a specific comic.
 *
 * @returns An object containing the bookmarks state and mutation methods.
 */
export function useBookmarks() {
	async function loadBookmarks() {
		if (isInitialized) return;
		isLoading = true;
		try {
			const [fetchedBookmarks, fetchedAssignments] = await Promise.all([
				invoke<Category[]>(BOOKMARKS_COMMANDS.getCategories),
				invoke<MangaCategory[]>(BOOKMARKS_COMMANDS.getAllComicCategories)
			]);
			bookmarks = fetchedBookmarks;
			assignments = fetchedAssignments;
			isInitialized = true;
		} catch (err) {
			error(`Failed to load bookmarks: ${err}`);
		} finally {
			isLoading = false;
		}
	}

	async function createBookmark(name: string, color: number) {
		try {
			const newBookmark = await invoke<Category>(BOOKMARKS_COMMANDS.createCategory, {
				name,
				color
			});
			bookmarks = [...bookmarks, newBookmark];
			return newBookmark;
		} catch (err) {
			error(`Failed to create bookmark: ${err}`);
			throw err;
		}
	}

	async function deleteBookmark(id: number) {
		try {
			await invoke(BOOKMARKS_COMMANDS.deleteCategory, { id: Number(id) });
			bookmarks = bookmarks.filter((bookmark) => bookmark.id !== id);
		} catch (err) {
			error(`Failed to delete bookmark: ${err}`);
			throw err;
		}
	}

	async function assignToComic(comicId: string | number, categoryId: number) {
		try {
			await invoke(BOOKMARKS_COMMANDS.removeCategoryFromComic, { comicId: comicId.toString() });
			const assignment = await invoke<MangaCategory>(BOOKMARKS_COMMANDS.assignCategoryToComic, {
				comicId: comicId.toString(),
				categoryId: Number(categoryId)
			});

			const comicIdStr = String(comicId);
			assignments = [
				...assignments.filter((a) => String(a.comic_directory_fk) !== comicIdStr),
				assignment
			];
			return assignment;
		} catch (err: unknown) {
			const msg = typeof err === 'object' && err !== null ? JSON.stringify(err) : String(err);
			error(`Failed to assign bookmark: ${msg}`);
			throw err;
		}
	}

	async function removeComicBookmark(comicId: string | number) {
		try {
			await invoke(BOOKMARKS_COMMANDS.removeCategoryFromComic, { comicId: comicId.toString() });
			const comicIdStr = String(comicId);
			assignments = assignments.filter((a) => String(a.comic_directory_fk) !== comicIdStr);
		} catch (err) {
			error(`Failed to remove bookmark from comic: ${err}`);
			throw err;
		}
	}

	async function getComicBookmark(comicId: string | number) {
		try {
			return await invoke<Category | null>(BOOKMARKS_COMMANDS.getComicCategory, {
				comicId: comicId.toString()
			});
		} catch (err) {
			error(`Failed to get comic bookmark: ${err}`);
			return null;
		}
	}

	function getBookmarkForComic(comicId: string | number) {
		const comicIdStr = String(comicId);
		const assignment = assignments.find((a) => String(a.comic_directory_fk) === comicIdStr);
		if (!assignment) return null;
		return bookmarks.find((b) => b.id === assignment.category_id) ?? null;
	}

	return {
		get bookmarks() {
			return bookmarks;
		},
		get assignments() {
			return assignments;
		},
		get isLoading() {
			return isLoading;
		},
		loadBookmarks,
		createBookmark,
		deleteBookmark,
		assignToComic,
		removeComicBookmark,
		getComicBookmark,
		getBookmarkForComic
	};
}
