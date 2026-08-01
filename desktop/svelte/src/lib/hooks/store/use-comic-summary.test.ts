import { render } from '@testing-library/svelte';
import { tick } from 'svelte';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';
import { debug } from '@tauri-apps/plugin-log';
import { toast } from 'svelte-sonner';
import { notificationStore } from '$lib/components/acerola-notification/acerola-notification.svelte';
import { HOME_COMMANDS } from '$lib/contracts/home/home.commands';
import { HOME_EVENTS } from '$lib/contracts/home/home.events';
import type { ComicSummaryPayload } from '$lib/contracts/home/home.payloads';
import HookHarness from '../../../../tests/harness/hooks/rune-wrapper.svelte';
import { useComicSummary, _resetComicSummaryState } from './use-comic-summary.svelte';

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0));

vi.mock('@tauri-apps/api/core', () => ({
	invoke: vi.fn()
}));

vi.mock('@tauri-apps/api/event', () => ({
	listen: vi.fn()
}));

vi.mock('@tauri-apps/plugin-log', () => ({
	debug: vi.fn()
}));

vi.mock('svelte-sonner', () => ({
	toast: {
		error: vi.fn()
	}
}));

const invokeMock = vi.mocked(invoke);
const listenMock = vi.mocked(listen);

async function renderHook() {
	let hook: ReturnType<typeof useComicSummary> | undefined;

	render(HookHarness, {
		props: {
			create: useComicSummary,
			onReady: (value) => {
				hook = value as ReturnType<typeof useComicSummary>;
			}
		}
	});

	await tick();
	await Promise.resolve();

	return hook!;
}

function setupListeners() {
	const callbacks = new Map<string, (event: { payload: unknown }) => void>();
	const unlisteners = new Map<string, ReturnType<typeof vi.fn>>();

	listenMock.mockImplementation((event, callback) => {
		callbacks.set(String(event), callback as (event: { payload: unknown }) => void);
		const unlisten = vi.fn();
		unlisteners.set(String(event), unlisten);
		return Promise.resolve(unlisten);
	});

	return { callbacks, unlisteners };
}

function summaryPayload(): ComicSummaryPayload {
	return {
		total: 1,
		fetchedAt: '2026-06-07T12:00:00.000Z',
		comics: [
			{
				relations: { directoryId: 'dir-1', metadataId: null },
				filesystem: { folderName: 'Acerola' },
				metadata: {
					title: 'Acerola',
					externalSync: false,
					activeSource: null,
					chapterCount: 3
				},
				artwork: { cover: null, banner: null }
			}
		]
	};
}

describe('useComicSummary', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		notificationStore.clearAll();
		_resetComicSummaryState();
		invokeMock.mockResolvedValue(undefined);
	});

	it('exibe loading até receber evento de dados', async () => {
		const { callbacks, unlisteners } = setupListeners();
		const hook = await renderHook();

		const fetchPromise = hook.fetch();
		await flushPromises();

		expect(hook.loading).toBe(true);
		expect(invokeMock).toHaveBeenCalledWith(HOME_COMMANDS.getComicSummarySorted, {
			search: undefined,
			sortBy: 'title',
			sortOrder: 'asc',
			showHidden: false,
			metadataSource: null
		});

		callbacks.get(HOME_EVENTS.homeData)?.({ payload: summaryPayload() });
		await fetchPromise;

		expect(hook.loading).toBe(false);
		expect(hook.comics?.total).toBe(1);
		expect(debug).toHaveBeenCalled();
		expect(unlisteners.get(HOME_EVENTS.homeData)).toHaveBeenCalledOnce();
		expect(unlisteners.get(HOME_EVENTS.homeError)).toHaveBeenCalledOnce();
	});

	it('ignora nova busca enquanto loading está ativo', async () => {
		const { callbacks } = setupListeners();
		const hook = await renderHook();

		const fetchPromise1 = hook.fetch();
		await flushPromises();

		invokeMock.mockClear();
		const fetchPromise2 = hook.fetch();

		expect(invokeMock).not.toHaveBeenCalled();

		callbacks.get(HOME_EVENTS.homeData)?.({ payload: summaryPayload() });
		await fetchPromise1;

		await flushPromises();
		callbacks.get(HOME_EVENTS.homeData)?.({ payload: summaryPayload() });
		await fetchPromise2;
	});

	it('exibe erro quando evento de erro é recebido', async () => {
		const { callbacks } = setupListeners();
		const hook = await renderHook();

		const fetchPromise = hook.fetch();
		await flushPromises();

		callbacks.get(HOME_EVENTS.homeError)?.({
			payload: { errorType: 'Unknown', message: 'falha ao carregar' }
		});
		await fetchPromise;

		expect(hook.loading).toBe(false);
		expect(hook.comics).toBeUndefined();
		expect(toast.error).toHaveBeenCalledWith('falha ao carregar');
		expect(notificationStore.notifications[0]?.message).toBe('Erro ao carregar biblioteca');
	});
});
