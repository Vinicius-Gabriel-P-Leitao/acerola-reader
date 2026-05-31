import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useComicChapters } from './use-comic-chapters.svelte';
import { LIBRARY_EVENTS } from '$lib/contracts/library/chapter.events';
import { mockIPC, mockWindows } from '@tauri-apps/api/mocks';
import { listen } from '@tauri-apps/api/event';

// Mock Tauri event tools
vi.mock('@tauri-apps/api/event', () => ({
	listen: vi.fn()
}));

describe('useComicChapters (Hook Integration)', () => {
	let eventCallback: Function;

	beforeEach(() => {
		vi.clearAllMocks();
		mockWindows('main');
		mockIPC(async () => {
			return;
		});

		// Capture the listener callback to simulate Rust events functionally
		(listen as any).mockImplementation((event: string, callback: Function) => {
			if (event === LIBRARY_EVENTS.comicChapters) {
				eventCallback = callback;
			}
			return Promise.resolve(() => {});
		});
	});

	const generateMockChapterData = (
		pageIndex: number,
		totalItems: number,
		itemsInPage: number = 25
	) => ({
		archive: {
			items: Array.from({ length: itemsInPage }, (_, itemIndex) => ({
				id: `id-${pageIndex}-${itemIndex}`,
				name: `Chapter ${pageIndex * 25 + itemIndex}`,
				path: `path-${pageIndex}-${itemIndex}`,
				chapterSort: `${pageIndex * 25 + itemIndex}`,
				volumeId: null,
				volumeName: null,
				isSpecial: false,
				lastModified: 0
			})),
			volumes: [],
			pageSize: 25,
			page: pageIndex,
			total: totalItems,
			volumeSections: []
		},
		showVolumeHeaders: false,
		hasVolumeStructure: false,
		effectiveViewMode: 'CHAPTER' as const
	});

	it('should initialize with an undefined state and not loading', () => {
		const chapterHook = useComicChapters();
		expect(chapterHook.chapters).toBeUndefined();
		expect(chapterHook.loading).toBe(false);
	});

	it('should correctly load and store the first page of chapters', async () => {
		const chapterHook = useComicChapters();
		const fetchOperation = chapterHook.fetch('directory-id-1', 0, 25, true);

		// Simulate Rust response
		eventCallback({ payload: generateMockChapterData(0, 100) });
		await fetchOperation;

		expect(chapterHook.chapters?.archive.items.length).toBe(25);
		expect(chapterHook.chapters?.archive.page).toBe(0);
		expect(chapterHook.lruKeys).toContain(0);
	});

	it('should manage the sliding window (LRU) when multiple pages are loaded', async () => {
		const chapterHook = useComicChapters();
		const totalPagesToLoad = 12; // Current hook limit

		// Functionally load pages sequentially
		await Array.from({ length: totalPagesToLoad }).reduce(async (previousPromise, _, pageIndex) => {
			await previousPromise;
			const currentFetch = chapterHook.fetch('directory-id-1', pageIndex, 25, true);
			eventCallback({ payload: generateMockChapterData(pageIndex, 500) });
			return currentFetch;
		}, Promise.resolve());

		expect(chapterHook.lruKeys.length).toBe(12);
		expect(chapterHook.lruKeys).toContain(0);
		expect(chapterHook.lruKeys).toContain(11);

		// Load the 13th page to trigger eviction
		const evictionFetch = chapterHook.fetch('directory-id-1', 12, 25, true);
		eventCallback({ payload: generateMockChapterData(12, 500) });
		await evictionFetch;

		// Page 0 should have been evicted (oldest access)
		expect(chapterHook.lruKeys.length).toBe(12);
		expect(chapterHook.lruKeys).not.toContain(0);
		expect(chapterHook.lruKeys).toContain(12);
	});

	it('should detect a large jump and reset the cache to prevent gaps', async () => {
		const chapterHook = useComicChapters();

		// Initial page
		const initialFetch = chapterHook.fetch('directory-id-1', 0, 25, true);
		eventCallback({ payload: generateMockChapterData(0, 500) });
		await initialFetch;

		// Teleport jump to page 50
		const jumpFetch = chapterHook.fetch('directory-id-1', 50, 25, true);
		eventCallback({ payload: generateMockChapterData(50, 500) });
		await jumpFetch;

		// Cache should be reset and only contain page 50
		expect(chapterHook.lruKeys).toEqual([50]);
		expect(chapterHook.lruKeys).not.toContain(0);
	});

	it('should handle an empty chapter list result gracefully', async () => {
		const chapterHook = useComicChapters();

		const emptyFetch = chapterHook.fetch('directory-id-1', 0, 25, true);
		eventCallback({ payload: generateMockChapterData(0, 0, 0) });
		await emptyFetch;

		expect(chapterHook.chapters?.archive.total).toBe(0);
		expect(chapterHook.chapters?.archive.items.length).toBe(0);
	});

	it('should preserve prioritized pages using the touch method during scrolling', async () => {
		const chapterHook = useComicChapters();

		// Load page 0 and page 1
		await [0, 1].reduce(async (previousPromise, pageIndex) => {
			await previousPromise;
			const currentFetch = chapterHook.fetch('directory-id-1', pageIndex, 25, true);
			eventCallback({ payload: generateMockChapterData(pageIndex, 100) });
			return currentFetch;
		}, Promise.resolve());

		// Mark page 0 as "recently used" manually (scroll telemetry simulation)
		chapterHook.touch(0);

		// Fill the remaining cache slots (from 2 up to 12)
		await Array.from({ length: 11 }, (_, index) => index + 2).reduce(
			async (previousPromise, pageIndex) => {
				await previousPromise;
				const currentFetch = chapterHook.fetch('directory-id-1', pageIndex, 25, true);
				eventCallback({ payload: generateMockChapterData(pageIndex, 500) });
				return currentFetch;
			},
			Promise.resolve()
		);

		// Page 1 should have been evicted instead of Page 0 because of the touch() call
		expect(chapterHook.lruKeys).toContain(0);
		expect(chapterHook.lruKeys).not.toContain(1);
	});
});
