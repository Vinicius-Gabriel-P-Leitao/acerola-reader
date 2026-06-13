import { render } from '@testing-library/svelte';
import { tick } from 'svelte';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';
import { toast } from 'svelte-sonner';
import { notificationStore } from '$lib/components/acerola-notification/acerola-notification.svelte';
import { DIRECTORY_SCAN_COMMANDS } from '$lib/contracts/library/library.commands';
import { LIBRARY_EVENTS } from '$lib/contracts/library/library.events';
import HookHarness from '../../../../tests/harness/hooks/rune-wrapper.svelte';
import { useLibraryScanner } from './use-comic-scanner.svelte';

vi.mock('@tauri-apps/api/core', () => ({
	invoke: vi.fn()
}));

vi.mock('@tauri-apps/api/event', () => ({
	listen: vi.fn()
}));

vi.mock('svelte-sonner', () => ({
	toast: {
		error: vi.fn(),
		info: vi.fn(),
		success: vi.fn()
	}
}));

const invokeMock = vi.mocked(invoke);
const listenMock = vi.mocked(listen);

async function renderScanner(path: string | undefined) {
	let hook: ReturnType<typeof useLibraryScanner> | undefined;

	render(HookHarness, {
		props: {
			create: () => useLibraryScanner(DIRECTORY_SCAN_COMMANDS.refreshLibrary, () => path),
			onReady: (value) => {
				hook = value as ReturnType<typeof useLibraryScanner>;
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

describe('useLibraryScanner', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		notificationStore.clearAll();
		invokeMock.mockResolvedValue(undefined);
	});

	it('exibe erro quando não há pasta selecionada', async () => {
		const hook = await renderScanner(undefined);

		await hook.start();

		expect(hook.scanning).toBe(false);
		expect(invokeMock).not.toHaveBeenCalled();
		expect(toast.error).toHaveBeenCalledWith('Sem pasta selecionada.');
	});

	it('exibe progresso e conclui scan com sucesso', async () => {
		const { callbacks, unlisteners } = setupListeners();
		const hook = await renderScanner('C:/Comics');

		await hook.start();

		expect(hook.scanning).toBe(true);
		expect(invokeMock).toHaveBeenCalledWith(DIRECTORY_SCAN_COMMANDS.refreshLibrary, {
			path: 'C:/Comics'
		});

		callbacks.get(LIBRARY_EVENTS.scanProgress)?.({ payload: undefined });

		expect(toast.info).toHaveBeenCalledWith('Scan em andamento...');
		expect(notificationStore.notifications[0]?.message).toBe('Scan em andamento...');

		callbacks.get(LIBRARY_EVENTS.scanComplete)?.({ payload: undefined });

		expect(hook.scanning).toBe(false);
		expect(toast.success).toHaveBeenCalledWith('Scan concluído!');
		expect(notificationStore.notifications.at(-1)?.message).toBe('Scan concluído!');
		expect(unlisteners.get(LIBRARY_EVENTS.scanProgress)).toHaveBeenCalledOnce();
		expect(unlisteners.get(LIBRARY_EVENTS.scanComplete)).toHaveBeenCalledOnce();
		expect(unlisteners.get(LIBRARY_EVENTS.scanError)).toHaveBeenCalledOnce();
	});

	it('remove progresso e exibe erro quando scan falha', async () => {
		const { callbacks } = setupListeners();
		const hook = await renderScanner('C:/Comics');

		await hook.start();
		callbacks.get(LIBRARY_EVENTS.scanProgress)?.({ payload: undefined });
		callbacks.get(LIBRARY_EVENTS.scanError)?.({
			payload: { errorType: 'Unknown', message: 'falha no scan' }
		});

		expect(hook.scanning).toBe(false);
		expect(toast.error).toHaveBeenCalledWith('falha no scan');
		expect(
			notificationStore.notifications.some((item) => item.message === 'Scan em andamento...')
		).toBe(false);
		expect(notificationStore.notifications[0]?.message).toBe('Falha no scan');
	});
});
