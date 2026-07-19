import { render } from '@testing-library/svelte';
import { tick } from 'svelte';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { invoke } from '@tauri-apps/api/core';
import { error as tauriError } from '@tauri-apps/plugin-log';
import HookHarness from '../../../../tests/harness/hooks/rune-wrapper.svelte';
import { useMetadataSync } from './use-metadata-sync.svelte';
import { METADATA_COMMANDS } from '$lib/contracts/metadata/metadata.commands';

vi.mock('@tauri-apps/api/core', () => ({
	invoke: vi.fn()
}));

vi.mock('@tauri-apps/plugin-log', () => ({
	error: vi.fn()
}));

vi.mock('@tauri-apps/plugin-store', () => ({
	load: vi.fn().mockResolvedValue({
		get: vi.fn().mockResolvedValue('test')
	})
}));

const invokeMock = vi.mocked(invoke);
const errorMock = vi.mocked(tauriError);

async function renderMetadataSyncHook() {
	let hook: ReturnType<typeof useMetadataSync> | undefined;

	render(HookHarness, {
		props: {
			create: () => useMetadataSync(),
			onReady: (value) => {
				hook = value as ReturnType<typeof useMetadataSync>;
			}
		}
	});

	await tick();
	await Promise.resolve();

	return hook!;
}

describe('useMetadataSync', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('sincroniza metadados com MangaDex com sucesso', async () => {
		const mockResponse = { id: '1', title: 'Test', hasComicInfo: false };
		invokeMock.mockResolvedValueOnce(mockResponse);

		const hook = await renderMetadataSyncHook();

		expect(hook.isSyncing).toBe(false);
		const result = await hook.syncMangadex('Naruto', '123');

		expect(invokeMock).toHaveBeenCalledWith(METADATA_COMMANDS.syncMangadex, {
			title: 'Naruto',
			comicId: '123',
			generateComicInfo: 'test',
			language: 'test'
		});
		expect(result).toEqual(mockResponse);
		expect(hook.isSyncing).toBe(false);
	});

	it('lida com erro ao sincronizar metadados com MangaDex', async () => {
		invokeMock.mockRejectedValueOnce('Network error');

		const hook = await renderMetadataSyncHook();

		await expect(hook.syncMangadex('Naruto', '123')).rejects.toEqual('Network error');

		expect(invokeMock).toHaveBeenCalledWith(METADATA_COMMANDS.syncMangadex, {
			title: 'Naruto',
			comicId: '123',
			generateComicInfo: 'test',
			language: 'test'
		});
		expect(errorMock).toHaveBeenCalledWith('Failed to sync MangaDex: "Network error"');
		expect(hook.isSyncing).toBe(false);
	});

	it('sincroniza metadados com AniList com sucesso', async () => {
		const mockResponse = { id: '2', title: 'Test 2', hasComicInfo: false };
		invokeMock.mockResolvedValueOnce(mockResponse);

		const hook = await renderMetadataSyncHook();

		expect(hook.isSyncing).toBe(false);
		const result = await hook.syncAnilist('Bleach', '456');

		expect(invokeMock).toHaveBeenCalledWith(METADATA_COMMANDS.syncAnilist, {
			title: 'Bleach',
			comicId: '456',
			generateComicInfo: 'test',
			language: 'test'
		});
		expect(result).toEqual(mockResponse);
		expect(hook.isSyncing).toBe(false);
	});

	it('lida com erro ao sincronizar metadados com AniList', async () => {
		invokeMock.mockRejectedValueOnce('API error');

		const hook = await renderMetadataSyncHook();

		await expect(hook.syncAnilist('Bleach', '456')).rejects.toEqual('API error');

		expect(invokeMock).toHaveBeenCalledWith(METADATA_COMMANDS.syncAnilist, {
			title: 'Bleach',
			comicId: '456',
			generateComicInfo: 'test',
			language: 'test'
		});
		expect(errorMock).toHaveBeenCalledWith('Failed to sync AniList: "API error"');
		expect(hook.isSyncing).toBe(false);
	});
});
