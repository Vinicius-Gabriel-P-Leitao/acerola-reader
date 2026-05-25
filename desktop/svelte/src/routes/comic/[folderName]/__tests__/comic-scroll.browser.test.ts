import { render } from 'vitest-browser-svelte';
import { expect, it, describe, vi, beforeEach } from 'vitest';
import ComicPage from '../+page.svelte';
import { LIBRARY_EVENTS } from '$lib/contracts/library/chapter.events';
import { LIBRARY_COMMANDS } from '$lib/contracts/library/chapter.commands';
import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';

// Mock Tauri APIs functionally
vi.mock('@tauri-apps/api/core', () => ({
	invoke: vi.fn(),
	convertFileSrc: vi.fn((path) => `asset://${path}`)
}));

vi.mock('@tauri-apps/api/event', () => ({
	listen: vi.fn()
}));

vi.mock('@tauri-apps/plugin-store', () => ({
	load: vi.fn().mockResolvedValue({
		get: vi.fn().mockResolvedValue('25'),
		set: vi.fn().mockResolvedValue(undefined),
		save: vi.fn().mockResolvedValue(undefined)
	})
}));

vi.mock('$lib/state/comic-context.svelte', () => ({
	useComicContext: vi.fn(() => ({
		item: null,
		coverUrl: null,
		set: vi.fn(),
		clear: vi.fn()
	}))
}));

vi.mock('$lib/assets/placeholder/placeholder_manga.svg?component', () => ({
	default: vi.fn(() => null)
}));

describe('ComicPage Scroll Integration', () => {
	const TOTAL_CHAPTERS = 1000;
	const PAGE_SIZE = 25;
	const ITEM_HEIGHT = 92; // 80px + gap
	let rustEventEmitter: Function;

	beforeEach(() => {
		vi.clearAllMocks();

		(listen as any).mockImplementation((eventName: string, callback: Function) => {
			if (eventName === LIBRARY_EVENTS.comicChapters) {
				rustEventEmitter = callback;
			}
			return Promise.resolve(() => {});
		});

		(invoke as any).mockImplementation(async () => ({}));
	});

	const generatePagePayload = (pageIndex: number) => ({
		archive: {
			items: Array.from({ length: PAGE_SIZE }, (_, index) => ({
				id: `id-${pageIndex}-${index}`,
				name: `Chapter ${pageIndex * PAGE_SIZE + index + 1}`,
				path: `path-${pageIndex}-${index}`,
				chapterSort: `${pageIndex * PAGE_SIZE + index + 1}`,
				volumeId: null,
				volumeName: null,
				isSpecial: false,
				lastModified: 0
			})),
			volumes: [],
			pageSize: PAGE_SIZE,
			page: pageIndex,
			total: TOTAL_CHAPTERS,
			volumeSections: []
		},
		showVolumeHeaders: false,
		hasVolumeStructure: false,
		effectiveViewMode: 'CHAPTER'
	});

	const loaderDataMock = {
		comic: {
			relations: { directoryId: '123', metadataId: null },
			filesystem: { folderName: 'test-manga' },
			metadata: {
				title: 'Production Test Manga',
				externalSync: false,
				activeSource: 'LOCAL',
				chapterCount: TOTAL_CHAPTERS
			},
			artwork: { cover: null, banner: null }
		},
		initialChaptersPerPage: '25'
	};

	it('should maintain scroll stability with virtual spacers during LRU eviction', async () => {
		const { container } = render(ComicPage, { data: loaderDataMock });

		// 1. Resolve initial fetch (Page 0)
		await vi.waitFor(() => expect(rustEventEmitter).toBeDefined());
		rustEventEmitter({ payload: generatePagePayload(0) });
		await new Promise((resolve) => setTimeout(resolve, 50));

		// 2. Load up to LRU limit (6 pages: 0, 1, 2, 3, 4, 5)
		await Array.from({ length: 6 }).reduce(async (promise, _, index) => {
			await promise;
			rustEventEmitter({ payload: generatePagePayload(index) });
			return new Promise((resolve) => setTimeout(resolve, 10));
		}, Promise.resolve());

		// 3. Evict Page 0 by loading Page 6
		rustEventEmitter({ payload: generatePagePayload(6) });
		await new Promise((resolve) => setTimeout(resolve, 100));

		// 4. Verify Virtual Padding Top (Page 0 is gone, so minPage=1)
		const listContainer = container.querySelector(".relative[style*='padding-top']") as HTMLElement;
		expect(listContainer).toBeDefined();

		// minPage (1) * pageSize (25) * itemHeight (92) = 2300px
		expect(listContainer.style.paddingTop).toBe('2300px');
	});

	it('should clear the cache when a non-adjacent page is received (Window Continuity)', async () => {
		const { container } = render(ComicPage, { data: loaderDataMock });

		await vi.waitFor(() => expect(rustEventEmitter).toBeDefined());

		// 1. Load Page 0
		rustEventEmitter({ payload: generatePagePayload(0) });
		await vi.waitFor(() => {
			const items = container.querySelectorAll('[data-slot="item-title"]');
			expect(items.length).toBe(PAGE_SIZE);
		});

		// 2. Receive Page 10 (Non-adjacent to Page 0)
		rustEventEmitter({ payload: generatePagePayload(10) });
		await new Promise((resolve) => setTimeout(resolve, 100));

		// 3. Verification: Page 0 should be GONE, and only Page 10 should be present
		const items = container.querySelectorAll('[data-slot="item-title"]');
		expect(items.length).toBe(PAGE_SIZE); // Only 25 items, not 50
		expect(items[0].textContent?.trim()).toBe('Chapter 251'); // Page 10 starts at item 250 (Chapter 251)

		// 4. Verify paddingTop is adjusted for Page 10
		const listContainer = container.querySelector(".relative[style*='padding-top']") as HTMLElement;
		// minPage (10) * pageSize (25) * itemHeight (92) = 23000px
		expect(listContainer.style.paddingTop).toBe('23000px');
	});

	it('should fetch the correct next page when scrolling down (Bidirectional Flow)', async () => {
		const { container } = render(ComicPage, { data: loaderDataMock });

		// 1. Initial State: Load Page 0
		await vi.waitFor(() => expect(rustEventEmitter).toBeDefined());
		rustEventEmitter({ payload: generatePagePayload(0) });

		// Ensure Page 0 is rendered
		await vi.waitFor(() => {
			const items = container.querySelectorAll('[data-slot="item-title"]');
			expect(items.length).toBe(PAGE_SIZE);
		});

		const scrollableElement = container.querySelector('.overflow-y-auto') as HTMLElement;
		expect(scrollableElement).toBeDefined();

		// Set explicit dimensions for the test environment
		Object.defineProperty(scrollableElement, 'clientHeight', { value: 800 });
		Object.defineProperty(scrollableElement, 'scrollHeight', { value: 92000 }); // 1000 items * 92px

		// 2. Simulate Scroll Down: Trigger Edge DOWN for Page 1
		// DISTANCE_THRESHOLD is 1500px.
		// pageSize=25. itemHeight=92. Page 0 ends at 2300px.
		// distanceFromContentBottom = (scrollHeight - paddingBottom) - (scrollTop + clientHeight)
		// (92000 - 89700) - (2000 + 800) = 2300 - 2800 = -500px (Trigger!)
		scrollableElement.scrollTop = 2000;
		scrollableElement.dispatchEvent(new Event('scroll'));

		// 3. Verification: System should request Page 1
		await vi.waitFor(
			() => {
				const invokeCalls = (invoke as any).mock.calls;
				const page1Requested = invokeCalls.some(
					(callArgs: any[]) =>
						callArgs[0] === LIBRARY_COMMANDS.getComicChapters && callArgs[1].page === 1
				);
				expect(page1Requested).toBe(true);
			},
			{ timeout: 5000 }
		);

		// 4. Provide Page 1 to the component
		rustEventEmitter({ payload: generatePagePayload(1) });
		await new Promise((resolve) => setTimeout(resolve, 100));

		// 5. Simulate Scroll Up: Trigger Edge UP for Page 0 (Already cached)
		// distanceFromContentTop = scrollTop - paddingTop = 100 - 0 = 100 (Trigger!)
		scrollableElement.scrollTop = 100;
		scrollableElement.dispatchEvent(new Event('scroll'));

		// Verification: Should NOT call invoke again for Page 0 (Cache HIT)
		// The touch() method should have been called instead
		const initialInvokeCount = (invoke as any).mock.calls.length;
		await new Promise((resolve) => setTimeout(resolve, 200));
		expect((invoke as any).mock.calls.length).toBe(initialInvokeCount);
	});

	it('should render correct chapter titles using the clean name from Rust', async () => {
		const { container } = render(ComicPage, { data: loaderDataMock });

		await vi.waitFor(() => expect(rustEventEmitter).toBeDefined());
		rustEventEmitter({ payload: generatePagePayload(0) });

		// Wait for the specific list component to render items
		// Item.Title uses data-slot="item-title"
		await vi.waitFor(
			() => {
				const chapterTitles = Array.from(
					container.querySelectorAll('[data-slot="item-title"]')
				).map((element) => element.textContent?.trim());
				expect(chapterTitles).toContain('Chapter 1');
			},
			{ timeout: 3000 }
		);
	});
});
