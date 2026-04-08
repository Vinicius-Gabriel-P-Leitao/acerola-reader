import { invoke } from "@tauri-apps/api/core";
import { load } from "@tauri-apps/plugin-store";
import { STORE_FILE, STORE_KEYS } from "$lib/constants/store-plugin";
import { toast } from "svelte-sonner";
import { COMMANDS } from "$lib/constants/commands";

export function useSelectFolder() {
  let folderPath = $state<string | undefined>(undefined);

  async function selectFolder() {
    const path = await invoke<string>(COMMANDS.selectFolder);

    if (path) {
      const store = await load(STORE_FILE);
      await store.set(STORE_KEYS.libraryPath, path);
      await store.save();

      // FIXME: Traduzir
      toast.success("Pasta salva com sucesso.");
      folderPath = path;
    }
  }

  async function loadSavedPath() {
    const store = await load(STORE_FILE);
    folderPath = (await store.get<string>(STORE_KEYS.libraryPath)) ?? undefined;
  }

  return {
    selectFolder,
    loadSavedPath,
    get folderPath() {
      return folderPath;
    },
  };
}
