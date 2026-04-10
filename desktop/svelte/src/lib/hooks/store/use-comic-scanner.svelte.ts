import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { toast } from "svelte-sonner";
import * as m from "$lib/paraglide/messages";
import { LIBRARY_COMMANDS } from "$lib/contracts/library/library.commands";
import { LIBRARY_EVENTS } from "$lib/contracts/library/library.events";
import type { ErrorPayload } from "$lib/contracts/shared/shared.payloads";
import { resolveErrorMessage } from "$lib/contracts/errors/errors.payloads";
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
      toast.error(m["hooks.comic_scanner.no_folder"]());
      return;
    }

    scanning = true;

    const unlistenProgress = await listen(LIBRARY_EVENTS.scanProgress, () => {
      if (progressId === undefined) {
        const msg = m["hooks.comic_scanner.in_progress"]();
        toast.info(msg);
        progressId = notify.info(msg, { duration: 0 });
      }
    });

    const unlisten = await listen(LIBRARY_EVENTS.scanComplete, () => {
      if (progressId !== undefined) {
        pop(progressId);
        progressId = undefined;
      }

      const msg = m["hooks.comic_scanner.success"]();
      notify.success(msg, { duration: 0 });
      toast.success(msg);

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

        const description = resolveErrorMessage(event.payload);
        notify.error(m["hooks.comic_scanner.error_title"](), {
          description,
          duration: 0,
        });

        toast.error(description);

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
