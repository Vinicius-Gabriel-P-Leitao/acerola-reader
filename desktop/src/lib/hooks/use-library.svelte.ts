import { invoke } from "@tauri-apps/api/core";
import { load } from "@tauri-apps/plugin-store";
import { STORE_FILE, STORE_KEYS } from "$lib/constants/store";

export function useLibrary() {
  let folderPath = $state<string | undefined>(undefined);

  async function selectFolder() {
    const path = await invoke<string>("select_folder");

    if (path) {
      const store = await load(STORE_FILE);
      await store.set(STORE_KEYS.libraryPath, path);
      await store.save();

      folderPath = path;
    }
  }

  async function loadSavedPath() {
    const store = await load(STORE_FILE);
    folderPath = (await store.get<string>(STORE_KEYS.libraryPath)) ?? undefined;
  }

  return {
    get folderPath() {
      return folderPath;
    },
    selectFolder,
    loadSavedPath,
  };
}
