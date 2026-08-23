import { load } from '@tauri-apps/plugin-store';
import { STORE_FILE, STORE_KEYS } from '$lib/constants/store-plugin';
import type { LanguageCode } from '$lib/constants/languages';

export function useMetadataLanguage() {
	let metadataLanguage = $state<LanguageCode>('pt-br');

	async function selectMetadataLanguage(value: LanguageCode) {
		const store = await load(STORE_FILE);
		await store.set(STORE_KEYS.metadataLanguage, value);
		await store.save();

		metadataLanguage = value;
	}

	async function loadSavedMetadataLanguage() {
		const store = await load(STORE_FILE);
		const saved = await store.get<LanguageCode>(STORE_KEYS.metadataLanguage);
		if (saved) {
			metadataLanguage = saved;
		}
	}

	return {
		selectMetadataLanguage,
		loadSavedMetadataLanguage,
		get metadataLanguage() {
			return metadataLanguage;
		}
	};
}
