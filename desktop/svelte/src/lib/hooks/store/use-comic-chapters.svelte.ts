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
  let chaptersData = $state<ChapterDto | undefined>(undefined);
  let loading = $state(false);

  async function fetch(
    comicDirectoryFk: string,
    page: number,
    pageSize: number,
    asc: boolean,
    volumeId: string | null = null,
    append: boolean = false,
  ) {
    if (loading) return;
    loading = true;

    const unlisten = await listen<ChapterDto>(
      LIBRARY_EVENTS.comicChapters,
      (event) => {
        const newData = event.payload;

        if (append && chaptersData) {
          // Append items and volume sections
          chaptersData.archive.items = [
            ...chaptersData.archive.items,
            ...newData.archive.items,
          ];

          // Merge volume sections (if they belong to the same volume or represent the same structure)
          // For infinite scroll within a volume, we mostly care about items.
          chaptersData.archive.volumeSections = newData.archive.volumeSections;

          // Update pagination info
          chaptersData.archive.page = newData.archive.page;
          chaptersData.archive.total = newData.archive.total;
        } else {
          chaptersData = newData;
        }

        loading = false;
        unlisten();
        unlistenErr();
      },
    );

    const unlistenErr = await listen<ErrorPayload>(
      LIBRARY_EVENTS.comicChaptersError,
      (event) => {
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
        volumeId,
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

  function clear() {
    chaptersData = undefined;
    loading = false;
  }

  return {
    fetch,
    clear,
    get chapters() {
      return chaptersData;
    },
    get loading() {
      return loading;
    },
  };
}
