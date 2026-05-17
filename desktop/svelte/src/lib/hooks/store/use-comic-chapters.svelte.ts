import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { toast } from "svelte-sonner";
import { LIBRARY_COMMANDS } from "$lib/contracts/library/chapter.commands";
import { LIBRARY_EVENTS } from "$lib/contracts/library/chapter.events";
import type { ChapterDto } from "$lib/contracts/library/chapter.payloads";
import type { ErrorPayload } from "$lib/contracts/shared/shared.payloads";
import { resolveErrorMessage } from "$lib/contracts/errors/errors.i18n";
import { notificationStore } from "$lib/components/acerola-notification/acerola-notification.svelte";

const { notify } = notificationStore;

export function useComicChapters() {
  let chapters = $state<ChapterDto | undefined>(undefined);
  let loading = $state(false);

  async function fetch(comicDirectoryFk: string, page: number, pageSize: number, asc: boolean) {
    if (loading) return;
    console.log(`[useComicChapters] Fetching chapters for comic ${comicDirectoryFk}, page ${page}, size ${pageSize}, asc ${asc}`);
    loading = true;

    const unlisten = await listen<ChapterDto>(
      LIBRARY_EVENTS.comicChapters,
      (event) => {
        console.log(`[useComicChapters] Received chapters:`, event.payload);
        chapters = event.payload;
        loading = false;
        unlisten();
        unlistenErr();
      },
    );

    const unlistenErr = await listen<ErrorPayload>(
      LIBRARY_EVENTS.comicChaptersError,
      (event) => {
        console.error(`[useComicChapters] Received error:`, event.payload);
        const description = resolveErrorMessage(event.payload);
        notify.error("Erro ao carregar capítulos", {
          description,
          duration: 0,
        });
        toast.error(description);
        loading = false;
        unlisten();
        unlistenErr();
      },
    );

    try {
      await invoke(LIBRARY_COMMANDS.getComicChapters, {
        comicDirectoryFk,
        page,
        pageSize,
        asc,
      });
    } catch (err) {
      console.error(`[useComicChapters] Invoke failed:`, err);
      loading = false;
      
      unlisten();
      unlistenErr();
    }
  }

  return {
    fetch,
    get chapters() {
      return chapters;
    },
    get loading() {
      return loading;
    },
  };
}
