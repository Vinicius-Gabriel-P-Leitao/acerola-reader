import { invoke } from '@tauri-apps/api/core';
import { load as loadStore } from '@tauri-apps/plugin-store';
import { STORE_FILE, STORE_KEYS } from '$lib/constants/store-plugin';
import { LIBRARY_COMMANDS } from '$lib/contracts/library/chapter.commands';
import type { ComicSummaryItemPayload } from '$lib/contracts/home/home.payloads';
import type { PageLoad } from './$types';

export const prerender = false;

export const load: PageLoad = async ({ params }) => {
	const comic = await invoke<ComicSummaryItemPayload | null>(
		LIBRARY_COMMANDS.getComicByFolderName,
		{
			folderName: params.folderName
		}
	);

	const store = await loadStore(STORE_FILE);
	const volumeViewMode = await store.get<'cover' | 'banner'>(STORE_KEYS.volumeViewMode);

	return {
		comic,
		initialVolumeViewMode: volumeViewMode
	};
};
