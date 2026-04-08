import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { toast } from "svelte-sonner";
import { COMMANDS } from "$lib/constants/commands";
import { notificationStore } from "$lib/components/acerola-notification/acerola-notification.svelte";

const { notify, pop } = notificationStore;

export function useComicScanner() {
  let scanning = $state(false);
  let progressId: number | undefined;
  let path = $state<string | undefined>(undefined);

  function init(folderPath: string) {
    path = folderPath;
  }

  async function startSpeedScanner() {
    if (!path) {
      // FIXME: Traduzir
      toast.error("Sem pasta selecionada.");
      return;
    }

    scanning = true;

    // FIXME: Criar contrato
    const unlistenProgress = await listen("scan:progress", () => {
      if (progressId === undefined) {
        // FIXME: Traduzir
        toast.info("Scan em andamento...");
        progressId = notify.info("Scan em andamento...", { duration: 0 });
      }
    });

    // FIXME: Criar contrato
    const unlisten = await listen("scan:complete", () => {
      if (progressId !== undefined) {
        pop(progressId);
        progressId = undefined;
      }

      // FIXME: Traduzir
      notify.success("Scan concluído!", { duration: 0 });
      toast.success("Scan concluído!");

      scanning = false;

      unlisten();
      unlistenErr();
      unlistenProgress();
    });

    // FIXME: Criar contrato
    const unlistenErr = await listen<string>("scan:error", (event) => {
      if (progressId !== undefined) {
        pop(progressId);
        progressId = undefined;
      }

      // FIXME: Traduzir
      notify.error("Falha no scan", {
        description: event.payload,
        duration: 0,
      });

      toast.error(event.payload);

      scanning = false;

      unlisten();
      unlistenErr();
      unlistenProgress();
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
