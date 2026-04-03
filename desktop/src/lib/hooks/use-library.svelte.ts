import { invoke } from "@tauri-apps/api/core";
import { load } from "@tauri-apps/plugin-store";

const STORE_FILE = "settings.json";
const LIBRARY_KEY = "library_path";

export function useLibrary() {
  let folderPath = $state<string | undefined>(undefined);

  async function selectFolder() {
    const path = await invoke<string>("select_folder");

    if (path) {
      const store = await load(STORE_FILE);
      await store.set(LIBRARY_KEY, path);

      await store.save();

      folderPath = path;
    }
  }

  async function loadSavedPath() {
    const store = await load(STORE_FILE);
    folderPath = (await store.get<string>(LIBRARY_KEY)) ?? undefined;
  }

  return {
    get folderPath() {
      return folderPath;
    },
    selectFolder,
    loadSavedPath,
  };
}
