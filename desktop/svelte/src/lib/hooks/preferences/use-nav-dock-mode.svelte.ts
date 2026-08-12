import { browser } from '$app/environment';
import { STORE_FILE, STORE_KEYS } from '$lib/constants/store-plugin';
import { LazyStore } from '@tauri-apps/plugin-store';

export type NavDockMode = 'fixed' | 'hover';

const store = new LazyStore(STORE_FILE);

let mode = $state<NavDockMode>('fixed');

// prettier-ignore
browser && (async () => {
    const saved = await store.get<NavDockMode>(STORE_KEYS.navDockMode);
    if (saved) mode = saved;
  })();

export function useNavDockMode() {
	async function setMode(newMode: NavDockMode) {
		mode = newMode;
		await store.set(STORE_KEYS.navDockMode, newMode);
		await store.save();
	}

	return {
		setMode,
		get mode() {
			return mode;
		}
	};
}
