import { render } from 'vitest-browser-svelte';
import { expect, it, describe, vi, beforeEach, afterEach } from 'vitest';
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

class MockIntersectionObserver {
	readonly root: Element | Document | null;
	readonly rootMargin: string;
	readonly thresholds: ReadonlyArray<number>;
	readonly observedElements = new Map<number, HTMLElement>();

	constructor(
		readonly callback: IntersectionObserverCallback,
		options?: IntersectionObserverInit
	) {
		this.root = options?.root ?? null;
		this.rootMargin = options?.rootMargin ?? '';
		this.thresholds = Array.isArray(options?.threshold)
			? options.threshold
			: [options?.threshold ?? 0];
	}

	observe = vi.fn((target: Element) => {
		const pageIndex = Number((target as HTMLElement).dataset.page);

		this.observedElements.set(pageIndex, target as HTMLElement);
	});

	unobserve = vi.fn((target: Element) => {
		const pageIndex = Number((target as HTMLElement).dataset.page);

		this.observedElements.delete(pageIndex);
	});

	disconnect = vi.fn(() => {
		this.observedElements.clear();
	});

	takeRecords = vi.fn(() => []);

	emitVisiblePages(pageIndexes: number[]) {
		const entries = Array.from(this.observedElements.entries()).map(([pageIndex, target]) => {
			const isIntersecting = pageIndexes.includes(pageIndex);
			const rect = target.getBoundingClientRect();

			return {
				boundingClientRect: rect,
				intersectionRatio: isIntersecting ? 1 : 0,
				intersectionRect: rect,
				isIntersecting,
				rootBounds: null,
				target,
				time: performance.now()
			} as IntersectionObserverEntry;
		});

		this.callback(entries, this as unknown as IntersectionObserver);
	}
}

describe('ComicPage Scroll Integration', () => {
	const TOTAL_CHAPTERS = 1000;
	const PAGE_SIZE = 25;
	const ITEM_HEIGHT = 112;

	let rustEventEmitter:
		| ((event: { payload: ReturnType<typeof generatePagePayload> }) => void)
		| undefined;
	let intersectionObservers: MockIntersectionObserver[] = [];

	beforeEach(() => {
		vi.clearAllMocks();
		rustEventEmitter = undefined;
		intersectionObservers = [];

		vi.stubGlobal(
			'IntersectionObserver',
			class extends MockIntersectionObserver {
				constructor(callback: IntersectionObserverCallback, options?: IntersectionObserverInit) {
					super(callback, options);
					intersectionObservers.push(this);
				}
			} as unknown as typeof IntersectionObserver
		);

		(listen as any).mockImplementation(
			(
				eventName: string,
				callback: (event: { payload: ReturnType<typeof generatePagePayload> }) => void
			) => {
				if (eventName === LIBRARY_EVENTS.comicChapters) {
					rustEventEmitter = callback;
				}
				return Promise.resolve(() => {});
			}
		);

		(invoke as any).mockImplementation(async () => ({}));
	});

	afterEach(() => {
		vi.unstubAllGlobals();
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
		initialChaptersPerPage: '25',
		initialVolumeViewMode: 'cover' as 'cover' | 'banner'
	};

	function getLatestObserver() {
		const observer = intersectionObservers[intersectionObservers.length - 1];

		if (!observer) {
			throw new Error('IntersectionObserver was not created');
		}

		return observer;
	}

	function emitComicChapters(pageIndex: number) {
		if (!rustEventEmitter) {
			throw new Error('Rust event emitter was not registered');
		}

		rustEventEmitter({ payload: generatePagePayload(pageIndex) });
	}

	function getChapterTitles(container: HTMLElement) {
		return Array.from(container.querySelectorAll('[data-slot="item-title"]')).map((element) =>
			element.textContent?.trim()
		);
	}

	function getPageRequestCount(pageIndex: number) {
		return (invoke as any).mock.calls.filter(
			(callArgs: any[]) =>
				callArgs[0] === LIBRARY_COMMANDS.getComicChapters && callArgs[1].page === pageIndex
		).length;
	}

	async function waitForPageRequest(pageIndex: number) {
		await vi.waitFor(() => {
			expect(getPageRequestCount(pageIndex)).toBeGreaterThan(0);
		});
	}

	async function waitForTrackedPage(pageIndex: number) {
		await vi.waitFor(() => {
			expect(getLatestObserver().observedElements.has(pageIndex)).toBe(true);
		});
	}

	async function waitForChapterTitle(container: HTMLElement, title: string) {
		await vi.waitFor(() => {
			expect(getChapterTitles(container)).toContain(title);
		});
	}

	async function renderLoadedComicPage() {
		const view = render(ComicPage, { data: loaderDataMock });

		await vi.waitFor(() => expect(rustEventEmitter).toBeDefined());
		await waitForPageRequest(0);
		emitComicChapters(0);
		await waitForChapterTitle(view.container, 'Chapter 1');
		await waitForTrackedPage(0);

		return view;
	}

	async function loadVisiblePage(container: HTMLElement, pageIndex: number) {
		await waitForTrackedPage(pageIndex);
		getLatestObserver().emitVisiblePages([pageIndex]);
		await waitForPageRequest(pageIndex);
		emitComicChapters(pageIndex);
		await waitForChapterTitle(container, `Chapter ${pageIndex * PAGE_SIZE + 1}`);
	}

	it('mantem a estabilidade do scroll com espacadores virtuais apos despejo do LRU', async () => {
		const { container } = await renderLoadedComicPage();

		for (let pageIndex = 1; pageIndex <= 6; pageIndex++) {
			await loadVisiblePage(container, pageIndex);
		}

		await vi.waitFor(() => {
			const titles = getChapterTitles(container);

			expect(titles).toHaveLength(PAGE_SIZE * 6);
			expect(titles).not.toContain('Chapter 1');
			expect(titles).toContain('Chapter 151');
		});

		const firstPageBlock = container.querySelector('[data-page="0"]') as HTMLElement | null;

		expect(firstPageBlock).not.toBeNull();
		expect(firstPageBlock?.style.height).toBe(`${PAGE_SIZE * ITEM_HEIGHT}px`);
		expect(firstPageBlock?.querySelector('[data-slot="item-title"]')).toBeNull();
	});

	it('limpa o cache quando recebe uma pagina nao adjacente', async () => {
		const { container } = await renderLoadedComicPage();

		await loadVisiblePage(container, 10);

		await vi.waitFor(() => {
			const titles = getChapterTitles(container);

			expect(titles).toHaveLength(PAGE_SIZE);
			expect(titles[0]).toBe('Chapter 251');
			expect(titles).not.toContain('Chapter 1');
		});

		const tenthPageBlock = container.querySelector('[data-page="10"]') as HTMLElement | null;

		expect(tenthPageBlock).not.toBeNull();
		expect(tenthPageBlock?.style.height).toBe(`${PAGE_SIZE * ITEM_HEIGHT}px`);
	});

	it('busca a proxima pagina correta ao rolar para baixo', async () => {
		const { container } = await renderLoadedComicPage();
		const initialPageZeroRequests = getPageRequestCount(0);

		await loadVisiblePage(container, 1);

		getLatestObserver().emitVisiblePages([0]);

		await new Promise((resolve) => setTimeout(resolve, 100));

		expect(getPageRequestCount(0)).toBe(initialPageZeroRequests);
		expect(getPageRequestCount(1)).toBe(1);
	});

	it('renderiza os titulos dos capitulos usando o nome limpo vindo do Rust', async () => {
		const { container } = await renderLoadedComicPage();

		await vi.waitFor(
			() => {
				expect(getChapterTitles(container)).toContain('Chapter 1');
			},
			{ timeout: 3000 }
		);
	});
});
