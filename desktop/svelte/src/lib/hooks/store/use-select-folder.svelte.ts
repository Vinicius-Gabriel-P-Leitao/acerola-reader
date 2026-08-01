import { browser } from '$app/environment';
import { notificationStore } from '$lib/components/acerola-notification/acerola-notification.svelte';
import { STORE_FILE, STORE_KEYS } from '$lib/constants/store-plugin';
import { LIBRARY_COMMANDS } from '$lib/contracts/library/library.commands';
import { m } from '$lib/paraglide/messages';
import { invoke } from '@tauri-apps/api/core';
import { load } from '@tauri-apps/plugin-store';
import { toast } from 'svelte-sonner';

const { notify } = notificationStore;

let folderPath = $state<string | undefined>(undefined);

async function loadSavedPath() {
	try {
		const store = await load(STORE_FILE);
		if (store) {
			if (typeof store.reload === 'function') {
				await store.reload();
			}
			const val = await store.get<string>(STORE_KEYS.libraryPath);
			folderPath = val || undefined;
		}
	} catch (err) {
		console.error('[useSelectFolder.loadSavedPath] error:', err);
	}
}

if (browser) {
	loadSavedPath();
}

export function useSelectFolder() {
	if (browser) {
		loadSavedPath();
	}
	async function selectFolder() {
		const path = await invoke<string>(LIBRARY_COMMANDS.selectFolder);

		if (path) {
			const store = await load(STORE_FILE);
			await store.set(STORE_KEYS.libraryPath, path);
			await store.save();

			notify.success(m['hooks.select_folder.success'](), { duration: 5000 });
			toast.success(m['hooks.select_folder.success']());
			folderPath = path;
		}
	}

	return {
		selectFolder,
		loadSavedPath,
		get folderPath() {
			return folderPath;
		}
	};
}
