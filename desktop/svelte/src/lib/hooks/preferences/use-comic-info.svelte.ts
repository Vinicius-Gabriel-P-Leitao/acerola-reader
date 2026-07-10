import { invoke } from '@tauri-apps/api/core';
import { load } from '@tauri-apps/plugin-store';
import { STORE_FILE, STORE_KEYS } from '$lib/constants/store-plugin';

export function useComicInfoPreference() {
	let comicInfoPreference = $state<boolean | undefined>(undefined);

	async function selectComicInfoPreference(value?: boolean) {
		const newValue = value ?? !comicInfoPreference;
		
		const store = await load(STORE_FILE);
		await store.set(STORE_KEYS.comicInfoPreference, newValue);
		await store.save();

		comicInfoPreference = newValue;
	}

	async function loadSavedComicInfoPreference() {
		const store = await load(STORE_FILE);
		comicInfoPreference = (await store.get<boolean>(STORE_KEYS.comicInfoPreference)) ?? false;
	}

	return {
		selectComicInfoPreference,
		loadSavedComicInfoPreference,
		get comicInfoPreference() {
			return comicInfoPreference;
		}
	};
}
