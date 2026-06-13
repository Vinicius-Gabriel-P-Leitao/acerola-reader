import { render } from '@testing-library/svelte';
import { tick } from 'svelte';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { invoke } from '@tauri-apps/api/core';
import { load } from '@tauri-apps/plugin-store';
import { toast } from 'svelte-sonner';
import { notificationStore } from '$lib/components/acerola-notification/acerola-notification.svelte';
import { STORE_KEYS } from '$lib/constants/store-plugin';
import { LIBRARY_COMMANDS } from '$lib/contracts/library/library.commands';
import HookHarness from '../../../../tests/harness/hooks/rune-wrapper.svelte';
import { useSelectFolder } from './use-select-folder.svelte';

vi.mock('@tauri-apps/api/core', () => ({
	invoke: vi.fn()
}));

vi.mock('@tauri-apps/plugin-store', () => ({
	load: vi.fn()
}));

vi.mock('svelte-sonner', () => ({
	toast: {
		success: vi.fn(),
		error: vi.fn(),
		info: vi.fn()
	}
}));

const invokeMock = vi.mocked(invoke);
const loadMock = vi.mocked(load);

async function renderHook() {
	let hook: ReturnType<typeof useSelectFolder> | undefined;

	render(HookHarness, {
		props: {
			create: useSelectFolder,
			onReady: (value) => {
				hook = value as ReturnType<typeof useSelectFolder>;
			}
		}
	});

	await tick();
	await Promise.resolve();

	return hook!;
}

function mockStore(values: Record<string, unknown> = {}) {
	const data = { ...values };
	const store = {
		get: vi.fn((key: string) => Promise.resolve(data[key] ?? null)),
		set: vi.fn((key: string, value: unknown) => {
			data[key] = value;
			return Promise.resolve();
		}),
		save: vi.fn(() => Promise.resolve())
	};

	loadMock.mockResolvedValue(store as never);

	return store;
}

describe('useSelectFolder', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		notificationStore.clearAll();
	});

	it('seleciona pasta, salva no store e expõe caminho', async () => {
		const store = mockStore();
		invokeMock.mockResolvedValue('C:/Comics');
		const hook = await renderHook();

		await hook.selectFolder();

		expect(invokeMock).toHaveBeenCalledWith(LIBRARY_COMMANDS.selectFolder);
		expect(hook.folderPath).toBe('C:/Comics');
		expect(store.set).toHaveBeenCalledWith(STORE_KEYS.libraryPath, 'C:/Comics');
		expect(store.save).toHaveBeenCalledOnce();
		expect(toast.success).toHaveBeenCalledWith('Pasta salva com sucesso.');
		expect(notificationStore.notifications).toHaveLength(1);
	});

	it('não salva quando seleção de pasta retorna vazia', async () => {
		const store = mockStore();
		invokeMock.mockResolvedValue('');
		const hook = await renderHook();

		await hook.selectFolder();

		expect(hook.folderPath).toBeUndefined();
		expect(store.set).not.toHaveBeenCalled();
		expect(toast.success).not.toHaveBeenCalled();
	});

	it('carrega caminho salvo do store', async () => {
		mockStore({ [STORE_KEYS.libraryPath]: 'D:/Library' });
		const hook = await renderHook();

		await hook.loadSavedPath();

		expect(hook.folderPath).toBe('D:/Library');
	});
});
