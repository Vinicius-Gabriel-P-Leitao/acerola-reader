import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { debug } from "@tauri-apps/plugin-log";
import { toast } from "svelte-sonner";
import { HOME_COMMANDS } from "$lib/contracts/home/home.commands";
import { HOME_EVENTS } from "$lib/contracts/home/home.events";
import type { ComicSummaryPayload } from "$lib/contracts/home/home.payloads";
import type { ErrorPayload } from "$lib/contracts/shared/shared.payloads";
import { resolveErrorMessage } from "$lib/contracts/errors/errors.i18n";
import { notificationStore } from "$lib/components/acerola-notification/acerola-notification.svelte";

const { notify } = notificationStore;

export function useComicSummary() {
  let comics = $state<ComicSummaryPayload | undefined>(undefined);
  let loading = $state(false);

  async function fetch() {
    if (loading) return;
    loading = true;

    const unlisten = await listen<ComicSummaryPayload>(
      HOME_EVENTS.homeData,
      (event) => {
        comics = event.payload;
        loading = false;

        debug(
          `[useComicSummary] total=${event.payload.total} fetchedAt=${event.payload.fetchedAt} payload=${JSON.stringify(event.payload.comics.slice(0, 3))}`,
        );

        unlisten();
        unlistenErr();
      },
    );

    const unlistenErr = await listen<ErrorPayload>(
      HOME_EVENTS.homeError,
      (event) => {
        const description = resolveErrorMessage(event.payload);

        // FIXME: Adicionar chaves i18n para hooks.comic_summary.error_title
        notify.error("Erro ao carregar biblioteca", {
          description,
          duration: 0,
        });
        toast.error(description);

        loading = false;

        unlisten();
        unlistenErr();
      },
    );

    await invoke(HOME_COMMANDS.getComicSummary);
  }

  return {
    fetch,
    get comics() {
      return comics;
    },
    get loading() {
      return loading;
    },
  };
}
