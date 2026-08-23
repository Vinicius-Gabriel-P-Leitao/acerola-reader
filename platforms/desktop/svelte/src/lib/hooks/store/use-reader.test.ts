import { render } from '@testing-library/svelte';
import { tick } from 'svelte';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { READER_COMMANDS } from '$lib/contracts/reader/reader.commands';
import type {
	ReaderChapterPayload,
	ReaderPagePayload,
	ReaderSessionPayload
} from '$lib/contracts/reader/reader.payloads';
import { useReader } from './use-reader.svelte';
import ReaderHarness from '../../../../tests/harness/hooks/reader-store.svelte';
import { invoke } from '@tauri-apps/api/core';

vi.mock('@tauri-apps/api/core', () => ({
	invoke: vi.fn()
}));

const invokeMock = vi.mocked(invoke);

function chapter(id = 'chapter-1'): ReaderChapterPayload {
	return {
		id,
		name: `Chapter ${id}`,
		path: `C:/mangas/${id}.cbz`,
		chapterSort: '1',
		volumeId: null,
		volumeName: null,
		isSpecial: false,
		lastModified: 0
	};
}

function session(chapterPayload = chapter(), pageCount = 5): ReaderSessionPayload {
	return {
		chapter: chapterPayload,
		pageCount,
		currentPage: 0,
		cacheCapacity: 3
	};
}

function page(chapterId: string, index: number): ReaderPagePayload {
	return {
		chapterId,
		index,
		total: 5,
		mimeType: index % 2 === 0 ? 'image/jpeg' : 'image/png',
		bytes: [index, index + 1],
		cacheHit: false
	};
}

async function renderHook() {
	let reader: ReturnType<typeof useReader> | undefined;

	const result = render(ReaderHarness, {
		props: {
			onReady: (hook) => {
				reader = hook;
			}
		}
	});

	await tick();
	await Promise.resolve();

	return { reader: reader!, unmount: result.unmount };
}

async function waitMicrotasks() {
	await Promise.resolve();
	await tick();
	await Promise.resolve();
}

function calls(command: string) {
	return invokeMock.mock.calls.filter(([calledCommand]) => calledCommand === command);
}

describe('useReader', () => {
	beforeEach(() => {
		vi.clearAllMocks();

		Object.defineProperty(URL, 'createObjectURL', {
			value: vi.fn((blob: Blob) => `blob:${blob.type}:${blob.size}`),
			configurable: true
		});

		Object.defineProperty(URL, 'revokeObjectURL', {
			value: vi.fn(),
			configurable: true
		});

		invokeMock.mockImplementation(async (command, args) => {
			if (command === READER_COMMANDS.openChapter) {
				const payload = args as { chapter: ReaderChapterPayload };
				return session(payload.chapter);
			}

			if (command === READER_COMMANDS.loadPage) {
				const payload = args as { index: number };
				return page('chapter-1', payload.index);
			}

			return undefined;
		});
	});

	it('starts with no open session and not loading', async () => {
		const { reader } = await renderHook();

		expect(reader.session).toBeUndefined();
		expect(reader.pageCount).toBe(0);
		expect(reader.currentPage).toBe(0);
		expect(reader.loading).toBe(false);
		expect(reader.error).toBeNull();
	});

	it('opens chapter, clamps initial page and loads current page', async () => {
		const { reader } = await renderHook();

		const chapterPayload = chapter();
		const readerSession = await reader.open(chapterPayload, 10);
		await waitMicrotasks();

		expect(readerSession.chapter).toEqual(chapterPayload);
		expect(reader.session?.chapter.id).toBe('chapter-1');
		expect(reader.currentPage).toBe(4);
		expect(reader.current?.index).toBe(4);
		expect(reader.current?.mimeType).toBe('image/jpeg');
		expect(invokeMock).toHaveBeenCalledWith(READER_COMMANDS.openChapter, {
			chapter: chapterPayload
		});
		expect(invokeMock).toHaveBeenCalledWith(READER_COMMANDS.loadPage, {
			index: 4,
			setCurrent: true
		});
		expect(invokeMock).toHaveBeenCalledWith(READER_COMMANDS.prefetchWindow, {
			center: 4,
			radius: 2
		});
	});

	it('reuses cached page without requesting bytes from backend again', async () => {
		const { reader } = await renderHook();

		await reader.open(chapter(), 0);
		await waitMicrotasks();
		invokeMock.mockClear();

		const cachedPage = await reader.loadPage(0);

		expect(cachedPage?.index).toBe(0);
		expect(calls(READER_COMMANDS.loadPage)).toHaveLength(0);
		expect(invokeMock).toHaveBeenCalledWith(READER_COMMANDS.setCurrentPage, { index: 0 });
	});

	it('ignores invalid page without calling backend', async () => {
		const { reader } = await renderHook();

		await reader.open(chapter(), 0);
		await waitMicrotasks();
		invokeMock.mockClear();

		const invalidPage = await reader.loadPage(99);

		expect(invalidPage).toBeUndefined();
		expect(invokeMock).not.toHaveBeenCalled();
	});

	it('records error when opening fails', async () => {
		const { reader } = await renderHook();
		const error = new Error('file not found');

		invokeMock.mockRejectedValueOnce(error);

		await expect(reader.open(chapter())).rejects.toThrow(error);

		expect(reader.error).toBe('file not found');
		expect(reader.loading).toBe(false);
	});

	it('records error when page loading fails', async () => {
		const { reader } = await renderHook();
		const error = new Error('decode failed');

		await reader.open(chapter(), 4);
		await waitMicrotasks();
		invokeMock.mockClear();
		invokeMock.mockRejectedValueOnce(error);

		await expect(reader.loadPage(1)).rejects.toThrow(error);

		expect(reader.error).toBe('decode failed');
		expect(reader.loading).toBe(false);
		expect(invokeMock).toHaveBeenCalledWith(READER_COMMANDS.loadPage, {
			index: 1,
			setCurrent: true
		});
	});

	it('ignores stale page from previous session', async () => {
		const { reader } = await renderHook();
		let activeChapterId = 'chapter-1';
		let resolveStalePage: (payload: ReaderPagePayload) => void = () => {};

		invokeMock.mockImplementation((command, args) => {
			if (command === READER_COMMANDS.openChapter) {
				const payload = args as { chapter: ReaderChapterPayload };
				activeChapterId = payload.chapter.id;
				return Promise.resolve(session(payload.chapter));
			}

			if (command === READER_COMMANDS.loadPage) {
				const payload = args as { index: number; setCurrent: boolean };

				if (activeChapterId === 'chapter-1' && payload.index === 1 && payload.setCurrent) {
					return new Promise<ReaderPagePayload>((resolve) => {
						resolveStalePage = resolve;
					});
				}

				return Promise.resolve(page(activeChapterId, payload.index));
			}

			return Promise.resolve(undefined);
		});

		await reader.open(chapter('chapter-1'), 4);
		await waitMicrotasks();

		const staleLoad = reader.loadPage(1);
		await Promise.resolve();

		await reader.open(chapter('chapter-2'), 4);
		await waitMicrotasks();

		resolveStalePage(page('chapter-1', 1));

		await expect(staleLoad).resolves.toBeUndefined();
		expect(reader.session?.chapter.id).toBe('chapter-2');
		expect(reader.currentPage).toBe(4);
		expect(reader.pageAt(1)).toBeUndefined();
	});

	it('closes chapter, clears cache and revokes object urls', async () => {
		const { reader } = await renderHook();

		await reader.open(chapter(), 0);
		await waitMicrotasks();

		expect(reader.cacheKeys.length).toBeGreaterThan(0);

		await reader.close();

		expect(invokeMock).toHaveBeenCalledWith(READER_COMMANDS.closeChapter);
		expect(reader.session).toBeUndefined();
		expect(reader.currentPage).toBe(0);
		expect(reader.cacheKeys).toEqual([]);
		expect(URL.revokeObjectURL).toHaveBeenCalled();
	});

	it('cleans up resources when component unmounts', async () => {
		const { reader, unmount } = await renderHook();

		await reader.open(chapter(), 0);
		await waitMicrotasks();
		invokeMock.mockClear();

		unmount();
		await waitMicrotasks();

		expect(invokeMock).toHaveBeenCalledWith(READER_COMMANDS.closeChapter);
		expect(URL.revokeObjectURL).toHaveBeenCalled();
	});
});
