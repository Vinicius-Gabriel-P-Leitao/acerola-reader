import { render } from '@testing-library/svelte';
import { tick } from 'svelte';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { invoke } from '@tauri-apps/api/core';
import { error as tauriError } from '@tauri-apps/plugin-log';
import HookHarness from '../../../../tests/harness/hooks/rune-wrapper.svelte';
import { useBookmarks, _resetBookmarksState } from './use-bookmarks.svelte';
import { BOOKMARKS_COMMANDS } from '$lib/contracts/bookmarks/bookmarks.commands';

vi.mock('@tauri-apps/api/core', () => ({
	invoke: vi.fn()
}));

vi.mock('@tauri-apps/plugin-log', () => ({
	error: vi.fn()
}));

const invokeMock = vi.mocked(invoke);
const errorMock = vi.mocked(tauriError);

async function renderBookmarksHook() {
	let hook: ReturnType<typeof useBookmarks> | undefined;

	render(HookHarness, {
		props: {
			create: () => useBookmarks(),
			onReady: (value) => {
				hook = value as ReturnType<typeof useBookmarks>;
			}
		}
	});

	await tick();
	await Promise.resolve();

	return hook!;
}

describe('useBookmarks', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		_resetBookmarksState();
	});

	it('loads bookmarks successfully', async () => {
		const mockBookmarks = [{ id: 1, name: 'Favoritos', color: 0xfff44336 }];
		const mockAssignments = [{ id: 1, comic_directory_fk: 123, category_id: 1 }];

		invokeMock.mockResolvedValueOnce(mockBookmarks);
		invokeMock.mockResolvedValueOnce(mockAssignments);

		const hook = await renderBookmarksHook();

		expect(hook.isLoading).toBe(false);
		await hook.loadBookmarks();

		expect(invokeMock).toHaveBeenCalledWith(BOOKMARKS_COMMANDS.getCategories);
		expect(hook.bookmarks).toEqual(mockBookmarks);
		expect(hook.isLoading).toBe(false);
	});

	it('handles error when loading bookmarks', async () => {
		invokeMock.mockRejectedValueOnce('Network error');
		invokeMock.mockResolvedValueOnce([]);

		const hook = await renderBookmarksHook();
		await hook.loadBookmarks();

		expect(invokeMock).toHaveBeenCalledWith(BOOKMARKS_COMMANDS.getCategories);
		expect(errorMock).toHaveBeenCalledWith('Failed to load bookmarks: Network error');
		expect(hook.bookmarks).toEqual([]);
		expect(hook.isLoading).toBe(false);
	});

	it('creates bookmark and adds it to the list', async () => {
		const newBookmark = { id: 2, name: 'Lidos', color: 0xffe91e63 };
		invokeMock.mockResolvedValueOnce(newBookmark);

		const hook = await renderBookmarksHook();

		const result = await hook.createBookmark('Lidos', 0xffe91e63);

		expect(invokeMock).toHaveBeenCalledWith(BOOKMARKS_COMMANDS.createCategory, {
			name: 'Lidos',
			color: 0xffe91e63
		});
		expect(result).toEqual(newBookmark);
		expect(hook.bookmarks).toContainEqual(newBookmark);
	});

	it('deletes bookmark and removes it from the list', async () => {
		const mockBookmarks = [{ id: 1, name: 'Fav', color: 0xfff44336 }];
		invokeMock.mockResolvedValueOnce(mockBookmarks);
		invokeMock.mockResolvedValueOnce([]);

		const hook = await renderBookmarksHook();
		await hook.loadBookmarks(); // Populates list
		expect(hook.bookmarks).toHaveLength(1);

		invokeMock.mockResolvedValueOnce(undefined); // delete_category

		await hook.deleteBookmark(1);

		expect(invokeMock).toHaveBeenCalledWith(BOOKMARKS_COMMANDS.deleteCategory, { id: 1 });
		expect(hook.bookmarks).toHaveLength(0);
	});

	it('assigns bookmark to comic', async () => {
		invokeMock.mockResolvedValueOnce(undefined); // remove_category_from_comic
		const assignment = { id: 1, comic_directory_fk: 123, category_id: 1 };
		invokeMock.mockResolvedValueOnce(assignment); // assign_category_to_comic

		const hook = await renderBookmarksHook();
		const result = await hook.assignToComic(123, 1);

		expect(invokeMock).toHaveBeenCalledWith(BOOKMARKS_COMMANDS.removeCategoryFromComic, {
			comicId: '123'
		});
		expect(invokeMock).toHaveBeenCalledWith(BOOKMARKS_COMMANDS.assignCategoryToComic, {
			comicId: '123',
			categoryId: 1
		});
		expect(result).toEqual(assignment);
	});

	it('removes bookmark from comic', async () => {
		invokeMock.mockResolvedValueOnce(undefined); // remove_category_from_comic

		const hook = await renderBookmarksHook();
		await hook.removeComicBookmark(123);

		expect(invokeMock).toHaveBeenCalledWith(BOOKMARKS_COMMANDS.removeCategoryFromComic, {
			comicId: '123'
		});
	});

	it('gets comic bookmark', async () => {
		const mockBookmark = { id: 1, name: 'Fav', color: 0xfff44336 };
		invokeMock.mockResolvedValueOnce(mockBookmark); // get_comic_category

		const hook = await renderBookmarksHook();
		const result = await hook.getComicBookmark(123);

		expect(invokeMock).toHaveBeenCalledWith(BOOKMARKS_COMMANDS.getComicCategory, {
			comicId: '123'
		});
		expect(result).toEqual(mockBookmark);
	});
});
