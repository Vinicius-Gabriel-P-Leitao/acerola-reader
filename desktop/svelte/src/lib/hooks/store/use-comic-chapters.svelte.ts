import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { toast } from "svelte-sonner";
import { LIBRARY_COMMANDS } from "$lib/contracts/library/chapter.commands";
import { LIBRARY_EVENTS } from "$lib/contracts/library/chapter.events";
import type {
  ChapterDto,
  ChapterFileDto,
} from "$lib/contracts/library/chapter.payloads";
import type { ErrorPayload } from "$lib/contracts/shared/shared.payloads";
import { resolveErrorMessage } from "$lib/contracts/errors/errors.i18n";
import { notificationStore } from "$lib/components/acerola-notification/acerola-notification.svelte";
import { LRUService } from "$lib/services/lru.service";
import { onMount } from "svelte";

const { notify } = notificationStore;

/**
 * Tamanho fixo do sliding window em páginas.
 * O LRU mantém no máximo 6 páginas em RAM a qualquer momento.
 */
const LRU_WINDOW_SIZE = 6;

/**
 * Distância máxima (em páginas) entre a página recebida e o target atual
 * antes de descartar a resposta como stale.
 */
const STALE_RESPONSE_THRESHOLD = 5;

export function useComicChapters() {
  const lruCache = new LRUService<number, ChapterFileDto[]>({
    max: LRU_WINDOW_SIZE,
  });

  let cacheVersion = $state(0);
  let loading = $state(false);
  let currentRequestPage = $state<number | null>(null);
  let failedPages = new Set<number>();
  
  let metadata = $state<
    | (Omit<ChapterDto, "archive"> & {
        archive: Omit<ChapterDto["archive"], "items">;
      })
    | undefined
  >(undefined);

  function clear() {
    lruCache.clear();
    failedPages.clear();
    metadata = undefined;
    currentRequestPage = null;
    cacheVersion++;
    loading = false;
  }

  /**
   * Promove a recência de uma página no cache, impedindo que ela seja evictada
   * se ela ainda estiver sendo visualizada no Viewport.
   */
  function touch(pageIndex: number) {
    if (lruCache.has(pageIndex)) {
      lruCache.get(pageIndex);
    }
  }

  /**
   * Verifica se uma página recebida cria um gap no window contíguo atual.
   * Gaps geralmente ocorrem quando a pessoa salta bruscamente usando o scroll.
   */
  function hasWindowGap(incomingPage: number): boolean {
    const cachedKeys = lruCache.keys;
    if (cachedKeys.length === 0) return false;

    const currentMin = Math.min(...cachedKeys);
    const currentMax = Math.max(...cachedKeys);

    const gapAbove = incomingPage - (currentMax + 1);
    const gapBelow = currentMin - (incomingPage + 1);

    return gapAbove > 0 || gapBelow > 0;
  }

  onMount(() => {
    let unlistenChapters: (() => void) | undefined;
    let unlistenError: (() => void) | undefined;

    const setupListeners = async () => {
      unlistenChapters = await listen<ChapterDto>(
        LIBRARY_EVENTS.comicChapters,
        (event) => {
          const payload = event.payload;

          // 1. Descarta respostas de scrolls velhos
          if (currentRequestPage !== null) {
            const dist = Math.abs(payload.archive.page - currentRequestPage);
            if (dist > STALE_RESPONSE_THRESHOLD) {
              console.log(
                `[useComicChapters] Stale page ${payload.archive.page} discarded` +
                  ` (target: ${currentRequestPage}, dist: ${dist})`,
              );
              loading = false;
              return;
            }
          }

          // 2. Se for um jump distante, quebra o gap resetando o window
          if (hasWindowGap(payload.archive.page)) {
            console.log(
              `[useComicChapters] Window gap for page ${payload.archive.page}` +
                ` (cached: [${lruCache.keys.join(",")}]). Resetting window.`,
            );
            lruCache.clear();
          }

          // 3. O set insere na MRU position. O próprio lru evicta o + velho se estourar.
          lruCache.set(payload.archive.page, payload.archive.items);
          failedPages.delete(payload.archive.page);

          metadata = {
            showVolumeHeaders: payload.showVolumeHeaders,
            hasVolumeStructure: payload.hasVolumeStructure,
            effectiveViewMode: payload.effectiveViewMode,
            archive: {
              page: payload.archive.page,
              pageSize: payload.archive.pageSize,
              total: payload.archive.total,
              volumes: payload.archive.volumes,
              volumeSections: payload.archive.volumeSections,
            },
          };

          cacheVersion++;
          loading = false;
        },
      );

      unlistenError = await listen<ErrorPayload>(
        LIBRARY_EVENTS.comicChaptersError,
        (event) => {
          const errorMessage = resolveErrorMessage(event.payload);
          notify.error("Erro ao carregar capítulos", {
            description: errorMessage,
          });
          toast.error(errorMessage);
          loading = false;
        },
      );
    };

    setupListeners();

    return () => {
      unlistenChapters?.();
      unlistenError?.();
    };
  });

  async function fetch(
    comicDirectoryId: string,
    pageIndex: number,
    pageSize: number,
    isAscending: boolean,
    volumeId: string | null = null,
  ) {
    if (lruCache.has(pageIndex)) return;
    if (failedPages.has(pageIndex)) return; // Evita infinite loops caso backend falhe
    if (loading) return;

    const totalItems = metadata?.archive.total ?? 0;
    if (totalItems > 0 && pageIndex * pageSize >= totalItems) return;

    loading = true;
    currentRequestPage = pageIndex;

    try {
      await invoke(LIBRARY_COMMANDS.getComicChapters, {
        comicDirectoryFk: comicDirectoryId,
        volumeId,
        page: pageIndex,
        pageSize,
        asc: isAscending,
      });
    } catch (error) {
      console.error("[useComicChapters] invoke failed:", error);
      failedPages.add(pageIndex);
      loading = false;
    }
  }

  const chapters = $derived.by(() => {
    cacheVersion;

    if (!metadata) return undefined;

    // Lemos as páginas em cache sem alterar recência, juntamos, e ordenamos de fato
    const allSortedItems = lruCache.keys
      .sort((a, b) => a - b)
      .flatMap((key) => lruCache.peek(key) || []);

    return {
      ...metadata,
      archive: {
        ...metadata.archive,
        items: allSortedItems,
      },
    } as ChapterDto;
  });

  return {
    fetch,
    clear,
    touch,
    get chapters() {
      return chapters;
    },
    get loading() {
      return loading;
    },
    get lruKeys() {
      return lruCache.keys;
    },
  };
}
