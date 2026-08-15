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

vi.mock('@tauri-apps/plugin-log', () => ({
	debug: vi.fn()
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

	const generateMockChapterData = (totalItems: number) => ({
		archive: {
			items: Array.from({ length: totalItems }, (_, itemIndex) => ({
				id: `id-${itemIndex}`,
				name: `Chapter ${itemIndex}`,
				path: `path-${itemIndex}`,
				chapterSort: `${itemIndex}`,
				volumeId: null,
				volumeName: null,
				isSpecial: false,
				lastModified: 0
			})),
			volumes: [],
			pageSize: totalItems,
			page: 0,
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

	it('busca e armazena todos os capítulos numa única resposta', async () => {
		const chapterHook = await renderComicChaptersHook();
		const fetchOperation = chapterHook.fetch('directory-id-1', 'number_asc');

		eventCallback({ payload: generateMockChapterData(400) });
		await fetchOperation;

		expect(chapterHook.chapters?.archive.items.length).toBe(400);
		expect(chapterHook.chapters?.archive.total).toBe(400);
	});

	it('pede a página 0 com um pageSize bem alto — sem paginação client-side', async () => {
		const chapterHook = await renderComicChaptersHook();
		const fetchOperation = chapterHook.fetch('directory-id-1', 'number_asc');

		eventCallback({ payload: generateMockChapterData(10) });
		await fetchOperation;

		expect(ipcCalls).toHaveLength(1);
		expect(ipcCalls[0]).toMatchObject({
			command: LIBRARY_COMMANDS.getComicChapters,
			args: expect.objectContaining({ page: 0 })
		});
		expect((ipcCalls[0].args as any).pageSize).toBeGreaterThan(10000);
	});

	it('não solicita novamente após uma falha de IPC', async () => {
		mockIPC(async (command, args) => {
			ipcCalls.push({ command, args });
			throw new Error('falha parcial');
		});

		const chapterHook = await renderComicChaptersHook();

		await chapterHook.fetch('directory-id-1', 'number_asc');
		await chapterHook.fetch('directory-id-1', 'number_asc');

		expect(chapterHook.loading).toBe(false);
		expect(ipcCalls).toHaveLength(1);
	});

	it('reutiliza dado já carregado sem novo IPC', async () => {
		const chapterHook = await renderComicChaptersHook();
		const fetchOperation = chapterHook.fetch('directory-id-1', 'number_asc');

		eventCallback({ payload: generateMockChapterData(50) });
		await fetchOperation;

		expect(ipcCalls).toHaveLength(1);

		await chapterHook.fetch('directory-id-1', 'number_asc');

		expect(ipcCalls).toHaveLength(1);
		expect(chapterHook.chapters?.archive.items.length).toBe(50);
	});

	it('should handle an empty chapter list result gracefully', async () => {
		const chapterHook = await renderComicChaptersHook();

		const emptyFetch = chapterHook.fetch('directory-id-1', 'number_asc');
		eventCallback({ payload: generateMockChapterData(0) });
		await emptyFetch;

		expect(chapterHook.chapters?.archive.total).toBe(0);
		expect(chapterHook.chapters?.archive.items.length).toBe(0);
	});

	it('mantém archive.volumes disponível enquanto um clear(true) refaz a busca', async () => {
		const chapterHook = await renderComicChaptersHook();

		const fetchOperation = chapterHook.fetch('directory-id-1', 'number_asc');
		eventCallback({
			payload: {
				...generateMockChapterData(10),
				hasVolumeStructure: true,
				archive: {
					...generateMockChapterData(10).archive,
					volumes: [{ id: 'v1', name: 'Volume 1', chapterCount: 10 }]
				}
			}
		});
		await fetchOperation;

		expect(chapterHook.chapters?.archive.volumes).toHaveLength(1);

		// Simula o efeito de expandir um volume: o hook consumidor sempre
		// chama clear(true) antes de refazer a busca com o novo filtro.
		chapterHook.clear(true);

		// archive.volumes não pode sumir nesse meio-tempo — é o que fazia a
		// lista de volumes inteira desmontar e remontar a cada clique.
		expect(chapterHook.chapters?.archive.volumes).toHaveLength(1);
		expect(chapterHook.chapters?.archive.items).toEqual([]);
	});

	it('clear() descarta o estado em tela mas não o cache — dado ainda em memória volta sem IPC', async () => {
		const chapterHook = await renderComicChaptersHook();

		const fetchOperation = chapterHook.fetch('directory-id-1', 'number_asc');
		eventCallback({ payload: generateMockChapterData(10) });
		await fetchOperation;

		expect(chapterHook.chapters?.archive.items.length).toBe(10);

		chapterHook.clear();

		expect(chapterHook.chapters).toBeUndefined();

		// Mesma chave (comic + ordenação) já foi buscada nesta sessão — deve
		// aplicar na hora, sem round-trip, exatamente o que evita a tela em
		// branco ao reabrir um volume/ordenação já visto.
		await chapterHook.fetch('directory-id-1', 'number_asc');

		expect(ipcCalls).toHaveLength(1);
		expect(chapterHook.chapters?.archive.items.length).toBe(10);
	});

	it('busca de novo via IPC quando a chave muda (volume diferente)', async () => {
		const chapterHook = await renderComicChaptersHook();

		const fetchOperation = chapterHook.fetch('directory-id-1', 'number_asc', 'volume-a');
		eventCallback({ payload: generateMockChapterData(10) });
		await fetchOperation;

		chapterHook.clear();

		const fetchOtherVolume = chapterHook.fetch('directory-id-1', 'number_asc', 'volume-b');
		eventCallback({ payload: generateMockChapterData(20) });
		await fetchOtherVolume;

		expect(ipcCalls).toHaveLength(2);
		expect(chapterHook.chapters?.archive.items.length).toBe(20);
	});

	it('reabrir um volume já visto aplica do cache sem round-trip', async () => {
		const chapterHook = await renderComicChaptersHook();

		const fetchA = chapterHook.fetch('directory-id-1', 'number_asc', 'volume-a');
		eventCallback({ payload: generateMockChapterData(10) });
		await fetchA;

		chapterHook.clear();

		const fetchB = chapterHook.fetch('directory-id-1', 'number_asc', 'volume-b');
		eventCallback({ payload: generateMockChapterData(20) });
		await fetchB;

		expect(ipcCalls).toHaveLength(2);

		chapterHook.clear();

		// volume-a já tinha sido buscado — volta do cache, sem IPC novo.
		await chapterHook.fetch('directory-id-1', 'number_asc', 'volume-a');

		expect(ipcCalls).toHaveLength(2);
		expect(chapterHook.chapters?.archive.items.length).toBe(10);
	});
});
