import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render } from '@testing-library/svelte';
import { tick } from 'svelte';
import { useComicChapters } from './use-comic-chapters.svelte';
import ComicChaptersHarness from '../../../../tests/harness/hooks/comic-chapters-store.svelte';
import { LIBRARY_COMMANDS } from '$lib/contracts/library/chapter.commands';
import { LIBRARY_EVENTS } from '$lib/contracts/library/chapter.events';
import { mockIPC, mockWindows } from '@tauri-apps/api/mocks';
import { listen } from '@tauri-apps/api/event';

// Mock Tauri event tools
vi.mock('@tauri-apps/api/event', () => ({
	listen: vi.fn()
}));

vi.mock('svelte-sonner', () => ({
	toast: {
		error: vi.fn()
	}
}));

describe('useComicChapters (Hook Integration)', () => {
	let eventCallback: Function;
	let ipcCalls: Array<{ command: string; args: unknown }>;

	async function renderComicChaptersHook() {
		let chapterHook: ReturnType<typeof useComicChapters> | undefined;

		render(ComicChaptersHarness, {
			props: {
				onReady: (hook) => {
					chapterHook = hook;
				}
			}
		});

		await tick();
		await Promise.resolve();

		return chapterHook!;
	}

	beforeEach(() => {
		vi.clearAllMocks();
		mockWindows('main');
		ipcCalls = [];
		mockIPC(async (command, args) => {
			ipcCalls.push({ command, args });
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

	it('should initialize with an undefined state and not loading', async () => {
		const chapterHook = await renderComicChaptersHook();
		expect(chapterHook.chapters).toBeUndefined();
		expect(chapterHook.loading).toBe(false);
	});

	it('should correctly load and store the first page of chapters', async () => {
		const chapterHook = await renderComicChaptersHook();
		const fetchOperation = chapterHook.fetch('directory-id-1', 0, 25, 'number_asc');

		eventCallback({ payload: generateMockChapterData(0, 100) });
		await fetchOperation;

		expect(chapterHook.chapters?.archive.items.length).toBe(25);
		expect(chapterHook.chapters?.archive.page).toBe(0);
		expect(chapterHook.lruKeys).toContain(0);
	});

	it('descarta resposta stale distante da página solicitada', async () => {
		const chapterHook = await renderComicChaptersHook();
		const fetchOperation = chapterHook.fetch('directory-id-1', 0, 25, 'number_asc');

		eventCallback({ payload: generateMockChapterData(6, 500) });
		await fetchOperation;

		expect(chapterHook.loading).toBe(false);
		expect(chapterHook.chapters).toBeUndefined();
		expect(chapterHook.lruKeys).toEqual([]);
	});

	it('não solicita novamente página marcada com erro parcial', async () => {
		mockIPC(async (command, args) => {
			ipcCalls.push({ command, args });
			throw new Error('falha parcial');
		});

		const chapterHook = await renderComicChaptersHook();

		await chapterHook.fetch('directory-id-1', 2, 25, 'number_asc');
		await chapterHook.fetch('directory-id-1', 2, 25, 'number_asc');

		expect(chapterHook.loading).toBe(false);
		expect(ipcCalls).toHaveLength(1);
		expect(ipcCalls[0]).toMatchObject({
			command: LIBRARY_COMMANDS.getComicChapters,
			args: expect.objectContaining({ page: 2 })
		});
	});

	it('reutiliza página em cache sem novo IPC', async () => {
		const chapterHook = await renderComicChaptersHook();
		const fetchOperation = chapterHook.fetch('directory-id-1', 0, 25, 'number_asc');

		eventCallback({ payload: generateMockChapterData(0, 100) });
		await fetchOperation;

		expect(ipcCalls).toHaveLength(1);

		await chapterHook.fetch('directory-id-1', 0, 25, 'number_asc');

		expect(ipcCalls).toHaveLength(1);
		expect(chapterHook.chapters?.archive.items.length).toBe(25);
	});

	it('should manage the sliding window (LRU) when multiple pages are loaded', async () => {
		const chapterHook = await renderComicChaptersHook();
		const totalPagesToLoad = 6;

		await Array.from({ length: totalPagesToLoad }).reduce(async (previousPromise, _, pageIndex) => {
			await previousPromise;
			const currentFetch = chapterHook.fetch('directory-id-1', pageIndex, 25, 'number_asc');
			eventCallback({ payload: generateMockChapterData(pageIndex, 500) });
			return currentFetch;
		}, Promise.resolve());

		expect(chapterHook.lruKeys.length).toBe(6);
		expect(chapterHook.lruKeys).toContain(0);
		expect(chapterHook.lruKeys).toContain(5);

		const evictionFetch = chapterHook.fetch('directory-id-1', 6, 25, 'number_asc');
		eventCallback({ payload: generateMockChapterData(6, 500) });
		await evictionFetch;

		expect(chapterHook.lruKeys.length).toBe(6);
		expect(chapterHook.lruKeys).not.toContain(0);
		expect(chapterHook.lruKeys).toContain(6);
	});

	it('should detect a large jump and reset the cache to prevent gaps', async () => {
		const chapterHook = await renderComicChaptersHook();

		const initialFetch = chapterHook.fetch('directory-id-1', 0, 25, 'number_asc');
		eventCallback({ payload: generateMockChapterData(0, 500) });
		await initialFetch;

		const jumpFetch = chapterHook.fetch('directory-id-1', 10, 25, 'number_asc');
		eventCallback({ payload: generateMockChapterData(10, 500) });
		await jumpFetch;

		expect(chapterHook.lruKeys).toEqual([10]);
		expect(chapterHook.lruKeys).not.toContain(0);
	});

	it('should handle an empty chapter list result gracefully', async () => {
		const chapterHook = await renderComicChaptersHook();

		const emptyFetch = chapterHook.fetch('directory-id-1', 0, 25, 'number_asc');
		eventCallback({ payload: generateMockChapterData(0, 0, 0) });
		await emptyFetch;

		expect(chapterHook.chapters?.archive.total).toBe(0);
		expect(chapterHook.chapters?.archive.items.length).toBe(0);
	});

	it('should preserve prioritized pages using the touch method during scrolling', async () => {
		const chapterHook = await renderComicChaptersHook();

		await [0, 1].reduce(async (previousPromise, pageIndex) => {
			await previousPromise;
			const currentFetch = chapterHook.fetch('directory-id-1', pageIndex, 25, 'number_asc');
			eventCallback({ payload: generateMockChapterData(pageIndex, 100) });
			return currentFetch;
		}, Promise.resolve());

		chapterHook.touch(0);

		await Array.from({ length: 5 }, (_, index) => index + 2).reduce(
			async (previousPromise, pageIndex) => {
				await previousPromise;
				const currentFetch = chapterHook.fetch('directory-id-1', pageIndex, 25, 'number_asc');
				eventCallback({ payload: generateMockChapterData(pageIndex, 500) });
				return currentFetch;
			},
			Promise.resolve()
		);

		expect(chapterHook.lruKeys).toContain(0);
		expect(chapterHook.lruKeys).not.toContain(1);
	});
});
