import { render } from '@testing-library/svelte';
import { tick } from 'svelte';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { invoke } from '@tauri-apps/api/core';
import { load } from '@tauri-apps/plugin-store';
import { STORE_KEYS } from '$lib/constants/store-plugin';
import { LIBRARY_COMMANDS } from '$lib/contracts/library/library.commands';
import HookHarness from '../../../../tests/harness/hooks/rune-wrapper.svelte';
import { useChaptersPerPage } from './use-chapters-per-page.svelte';
import { useComicInfoPreference } from './use-comic-info.svelte';
import { useVolumeViewMode } from './use-volume-view-mode.svelte';

vi.mock('@tauri-apps/api/core', () => ({
	invoke: vi.fn()
}));

vi.mock('@tauri-apps/plugin-store', () => ({
	load: vi.fn()
}));

const invokeMock = vi.mocked(invoke);
const loadMock = vi.mocked(load);

type StoreMock = {
	get: ReturnType<typeof vi.fn>;
	set: ReturnType<typeof vi.fn>;
	save: ReturnType<typeof vi.fn>;
};

async function renderHook<T>(create: () => T) {
	let hook: T | undefined;

	render(HookHarness, {
		props: {
			create,
			onReady: (value) => {
				hook = value as T;
			}
		}
	});

	await tick();
	await Promise.resolve();

	return hook!;
}

function mockStore(values: Record<string, unknown> = {}) {
	const data = { ...values };
	const store: StoreMock = {
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

describe('useChaptersPerPage', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('carrega valor salvo de capítulos por página', async () => {
		mockStore({ [STORE_KEYS.chaptersPerPage]: '100' });
		const hook = await renderHook(useChaptersPerPage);

		await hook.loadChaptersPerPage();

		expect(hook.chaptersPerPage).toBe('100');
	});

	it('usa valor padrão quando não há capítulos por página salvos', async () => {
		mockStore();
		const hook = await renderHook(useChaptersPerPage);

		await hook.loadChaptersPerPage();

		expect(hook.chaptersPerPage).toBe('25');
	});

	it('salva capítulos por página e atualiza estado visível', async () => {
		const store = mockStore();
		const hook = await renderHook(useChaptersPerPage);

		await hook.saveChaptersPerPage('50');

		expect(hook.chaptersPerPage).toBe('50');
		expect(store.set).toHaveBeenCalledWith(STORE_KEYS.chaptersPerPage, '50');
		expect(store.save).toHaveBeenCalledOnce();
	});
});

describe('useVolumeViewMode', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('carrega modo de volume salvo', async () => {
		mockStore({ [STORE_KEYS.volumeViewMode]: 'banner' });
		const hook = await renderHook(useVolumeViewMode);

		await hook.loadVolumeViewMode();

		expect(hook.volumeViewMode).toBe('banner');
	});

	it('usa capa como modo padrão quando não há valor salvo', async () => {
		mockStore();
		const hook = await renderHook(useVolumeViewMode);

		await hook.loadVolumeViewMode();

		expect(hook.volumeViewMode).toBe('cover');
	});

	it('salva modo de volume e atualiza estado visível', async () => {
		const store = mockStore();
		const hook = await renderHook(useVolumeViewMode);

		await hook.saveVolumeViewMode('banner');

		expect(hook.volumeViewMode).toBe('banner');
		expect(store.set).toHaveBeenCalledWith(STORE_KEYS.volumeViewMode, 'banner');
		expect(store.save).toHaveBeenCalledOnce();
	});
});

describe('useComicInfoPreference', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('salva preferência escolhida quando IPC retorna verdadeiro', async () => {
		const store = mockStore();
		invokeMock.mockResolvedValue(true);
		const hook = await renderHook(useComicInfoPreference);

		await hook.selectComicInfoPreference();

		expect(invokeMock).toHaveBeenCalledWith(LIBRARY_COMMANDS.comicInfoPreference);
		expect(hook.comicInfoPreference).toBe(true);
		expect(store.set).toHaveBeenCalledWith(STORE_KEYS.comicInfoPreference, true);
		expect(store.save).toHaveBeenCalledOnce();
	});

	it('mantém indefinido quando IPC retorna falso', async () => {
		const store = mockStore();
		invokeMock.mockResolvedValue(false);
		const hook = await renderHook(useComicInfoPreference);

		await hook.selectComicInfoPreference();

		expect(hook.comicInfoPreference).toBeUndefined();
		expect(store.set).not.toHaveBeenCalled();
	});

	it('carrega preferência salva de comic info', async () => {
		mockStore({ [STORE_KEYS.comicInfoPreference]: true });
		const hook = await renderHook(useComicInfoPreference);

		await hook.loadSavedComicInfoPreference();

		expect(hook.comicInfoPreference).toBe(true);
	});
});
