import { invoke } from "@tauri-apps/api/core";
import { load } from "@tauri-apps/plugin-store";
import { STORE_FILE, STORE_KEYS } from "$lib/constants/store-plugin";
import { LIBRARY_COMMANDS } from "$lib/contracts/library/library.commands";

export function useComicInfoPreference() {
  let comicInfoPreference = $state<boolean | undefined>(undefined);

  async function selectComicInfoPreference() {
    const preference = await invoke<boolean>(LIBRARY_COMMANDS.comicInfoPreference);

    if (preference) {
      const store = await load(STORE_FILE);
      await store.set(STORE_KEYS.comicInfoPreference, preference);
      await store.save();

      comicInfoPreference = preference;
    }
  }

  async function loadSavedComicInfoPreference() {
    const store = await load(STORE_FILE);
    comicInfoPreference =
      (await store.get<boolean>(STORE_KEYS.libraryPath)) ?? undefined;
  }

  return {
    selectComicInfoPreference,
    loadSavedComicInfoPreference,
    get comicInfoPreference() {
      return comicInfoPreference;
    },
  };
}
