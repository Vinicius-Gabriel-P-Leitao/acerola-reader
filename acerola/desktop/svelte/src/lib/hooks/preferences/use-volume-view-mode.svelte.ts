import { STORE_FILE, STORE_KEYS } from '$lib/constants/store-plugin';
import { load } from '@tauri-apps/plugin-store';

type VolumeViewMode = 'cover' | 'banner';

export function useVolumeViewMode() {
	let volumeViewMode = $state<VolumeViewMode>('cover');

	async function loadVolumeViewMode() {
		const store = await load(STORE_FILE);

		volumeViewMode = (await store.get<VolumeViewMode>(STORE_KEYS.volumeViewMode)) ?? 'cover';
	}

	async function saveVolumeViewMode(value: VolumeViewMode) {
		const store = await load(STORE_FILE);

		volumeViewMode = value;

		await store.set(STORE_KEYS.volumeViewMode, value);
		await store.save();
	}

	return {
		loadVolumeViewMode,
		saveVolumeViewMode,

		get volumeViewMode() {
			return volumeViewMode;
		},

		set volumeViewMode(value: VolumeViewMode) {
			volumeViewMode = value;
		}
	};
}
