import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { toast } from "svelte-sonner";
import { LIBRARY_COMMANDS } from "$lib/contracts/library/library.commands";
import { LIBRARY_EVENTS } from "$lib/contracts/library/library.events";
import type { ErrorPayload } from "$lib/contracts/shared/shared.payloads";
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

    const unlistenProgress = await listen(LIBRARY_EVENTS.scanProgress, () => {
      if (progressId === undefined) {
        // FIXME: Traduzir
        toast.info("Scan em andamento...");
        progressId = notify.info("Scan em andamento...", { duration: 0 });
      }
    });

    const unlisten = await listen(LIBRARY_EVENTS.scanComplete, () => {
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

    const unlistenErr = await listen<ErrorPayload>(
      LIBRARY_EVENTS.scanError,
      (event) => {
        if (progressId !== undefined) {
          pop(progressId);
          progressId = undefined;
        }

        // FIXME: Traduzir
        notify.error("Falha no scan", {
          description: event.payload.message,
          duration: 0,
        });

        toast.error(event.payload.message);

        scanning = false;

        unlisten();
        unlistenErr();
        unlistenProgress();
      },
    );

    await invoke(LIBRARY_COMMANDS.comicScanner, { path });
  }

  return {
    init,
    startSpeedScanner,
    get scanning() {
      return scanning;
    },
  };
}
