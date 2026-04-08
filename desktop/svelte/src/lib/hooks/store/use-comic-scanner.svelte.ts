import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { load } from "@tauri-apps/plugin-store";
import { STORE_FILE, STORE_KEYS } from "$lib/constants/store";
import { toast } from "svelte-sonner";
import { COMMANDS } from "$lib/constants/commands";

export function useComicScanner() {
  let scanning = $state(false);
  let path = $state<string | undefined>(undefined);

  function init(folderPath: string) {
    path = folderPath;
  }

  async function startSpeedScanner(path: string) {
    // FIXME: Traduzir
    if (!path) {
      toast.error("Sem pasta selecionada.");
      return;
    }

    scanning = true;

    const unlisten = await listen("scan:complete", () => {
      toast.success("Scan concluído!");
      scanning = false;

      unlisten();
      unlistenErr();
    });

    const unlistenErr = await listen<string>("scan:error", (event) => {
      toast.error(`Erro no scan: ${event.payload}`);
      scanning = false;

      unlisten();
      unlistenErr();
    });

    await invoke(COMMANDS.comicScanner, { path });
  }

  return {
    init,
    startSpeedScanner,
    get scanning() {
      return scanning;
    },
  };
}
