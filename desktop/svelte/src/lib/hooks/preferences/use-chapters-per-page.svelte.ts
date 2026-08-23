import { STORE_FILE, STORE_KEYS } from '$lib/constants/store-plugin';
import { load } from '@tauri-apps/plugin-store';

export function useChaptersPerPage() {
	let chaptersPerPage = $state('25');

	async function loadChaptersPerPage() {
		const store = await load(STORE_FILE);

		chaptersPerPage = (await store.get<string>(STORE_KEYS.chaptersPerPage)) ?? '25';
	}

	async function saveChaptersPerPage(value: string) {
		const store = await load(STORE_FILE);

		chaptersPerPage = value;

		await store.set(STORE_KEYS.chaptersPerPage, value);
		await store.save();
	}

	return {
		loadChaptersPerPage,
		saveChaptersPerPage,

		get chaptersPerPage() {
			return chaptersPerPage;
		},

		set chaptersPerPage(value: string) {
			chaptersPerPage = value;
		}
	};
}
