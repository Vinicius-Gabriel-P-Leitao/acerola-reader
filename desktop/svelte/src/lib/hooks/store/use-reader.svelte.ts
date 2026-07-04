import { invoke } from '@tauri-apps/api/core';
import { onDestroy } from 'svelte';
import { READER_COMMANDS } from '$lib/contracts/reader/reader.commands';
import type {
	ReaderCachedPagePayload,
	ReaderChapterPayload,
	ReaderPagePayload,
	ReaderSessionPayload
} from '$lib/contracts/reader/reader.payloads';
import { LRUService } from '$lib/services/lru.service';

const DEFAULT_READER_CACHE_SIZE = 7;
const PREFETCH_RADIUS = 2;

export function useReader() {
	let pageCache = createPageCache(DEFAULT_READER_CACHE_SIZE);
	let pendingPages = new Map<number, Promise<ReaderCachedPagePayload | undefined>>();
	let requestVersion = 0;

	let session = $state<ReaderSessionPayload | undefined>(undefined);
	let currentPage = $state(0);
	let loading = $state(false);
	let error = $state<string | null>(null);
	let cacheVersion = $state(0);

	onDestroy(() => {
		pageCache.clear();
		void invoke(READER_COMMANDS.closeChapter).catch(() => undefined);
	});

	function createPageCache(max: number) {
		return new LRUService<number, ReaderCachedPagePayload>({
			max,
			dispose: (page) => URL.revokeObjectURL(page.url)
		});
	}

	function resetCache(max = DEFAULT_READER_CACHE_SIZE) {
		requestVersion++;
		pageCache.clear();
		pageCache = createPageCache(max);
		pendingPages.clear();
		cacheVersion++;
	}

	function setError(value: unknown) {
		error = value instanceof Error ? value.message : String(value);
	}

	function clampPage(index: number) {
		if (!session) return 0;
		return Math.max(0, Math.min(index, session.pageCount - 1));
	}

	function isValidPage(index: number) {
		return Boolean(session && index >= 0 && index < session.pageCount);
	}

	function toCachedPage(payload: ReaderPagePayload): ReaderCachedPagePayload {
		const bytes = new Uint8Array(payload.bytes);
		const blob = new Blob([bytes], { type: payload.mimeType });

		return {
			index: payload.index,
			total: payload.total,
			mimeType: payload.mimeType,
			url: URL.createObjectURL(blob),
			cacheHit: payload.cacheHit
		};
	}

	function windowIndices(center: number) {
		if (!session) return [];

		const start = Math.max(0, center - PREFETCH_RADIUS);
		const end = Math.min(session.pageCount - 1, center + PREFETCH_RADIUS);

		return Array.from({ length: end - start + 1 }, (_, index) => start + index);
	}

	async function open(chapter: ReaderChapterPayload, startPage = 0) {
		loading = true;
		error = null;
		resetCache();

		try {
			session = await invoke<ReaderSessionPayload>(READER_COMMANDS.openChapter, { chapter });
			resetCache(session.cacheCapacity);

			currentPage = clampPage(startPage);
			await goToPage(currentPage);

			return session;
		} catch (caught) {
			setError(caught);
			throw caught;
		} finally {
			loading = false;
		}
	}

	async function loadPage(index: number, setCurrent = true) {
		if (!isValidPage(index)) return undefined;

		const cached = setCurrent ? pageCache.get(index) : pageCache.peek(index);
		if (cached) {
			if (setCurrent) {
				await syncCurrentPage(index);
			}

			cacheVersion++;
			return cached;
		}

		const pending = pendingPages.get(index);
		if (pending) return pending;

		const version = requestVersion;
		const chapterId = session?.chapter.id;

		const request = (async () => {
			if (setCurrent) loading = true;

			try {
				const payload = await invoke<ReaderPagePayload>(READER_COMMANDS.loadPage, {
					index,
					setCurrent
				});

				if (
					version !== requestVersion ||
					payload.chapterId !== chapterId ||
					payload.chapterId !== session?.chapter.id
				) {
					return undefined;
				}

				const page = toCachedPage(payload);

				pageCache.set(index, page);

				if (setCurrent) {
					currentPage = index;
				}

				error = null;
				cacheVersion++;

				return page;
			} catch (caught) {
				if (version === requestVersion) {
					setError(caught);
				}
				throw caught;
			} finally {
				if (version === requestVersion) {
					pendingPages.delete(index);
				}
				if (setCurrent && version === requestVersion) loading = false;
			}
		})();

		pendingPages.set(index, request);
		return request;
	}

	async function syncCurrentPage(index: number) {
		if (!isValidPage(index)) return;

		currentPage = index;
		pageCache.get(index);
		cacheVersion++;

		try {
			await invoke(READER_COMMANDS.setCurrentPage, { index });
		} catch (caught) {
			setError(caught);
		}
	}

	async function prefetchWindow(center = currentPage) {
		if (!session) return;

		void invoke(READER_COMMANDS.prefetchWindow, {
			center,
			radius: PREFETCH_RADIUS
		}).catch((caught) => setError(caught));

		for (const index of windowIndices(center)) {
			if (index !== center && !pageCache.has(index)) {
				void loadPage(index, false).catch(() => undefined);
			}
		}
	}

	async function goToPage(index: number) {
		const nextPage = clampPage(index);
		const page = await loadPage(nextPage, true);

		if (page) {
			void prefetchWindow(nextPage);
		}

		return page;
	}

	function nextPage() {
		return goToPage(currentPage + 1);
	}

	function previousPage() {
		return goToPage(currentPage - 1);
	}

	async function close() {
		try {
			await invoke(READER_COMMANDS.closeChapter);
		} finally {
			session = undefined;
			currentPage = 0;
			loading = false;
			error = null;
			resetCache();
		}
	}

	const pages = $derived.by(() => {
		cacheVersion;

		return pageCache.keys
			.slice()
			.sort((left, right) => left - right)
			.map((key) => pageCache.peek(key))
			.filter((page): page is ReaderCachedPagePayload => Boolean(page));
	});

	const current = $derived.by(() => {
		cacheVersion;
		return pageCache.peek(currentPage);
	});

	const cacheKeys = $derived.by(() => {
		cacheVersion;
		return pageCache.keys;
	});

	return {
		open,
		loadPage,
		goToPage,
		nextPage,
		previousPage,
		prefetchWindow,
		close,
		pageAt(index: number) {
			cacheVersion;
			return pageCache.peek(index);
		},
		get session() {
			return session;
		},
		get pageCount() {
			return session?.pageCount ?? 0;
		},
		get currentPage() {
			return currentPage;
		},
		get current() {
			return current;
		},
		get pages() {
			return pages;
		},
		get cacheKeys() {
			return cacheKeys;
		},
		get loading() {
			return loading;
		},
		get error() {
			return error;
		}
	};
}
