import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { toast } from "svelte-sonner";
import { LIBRARY_COMMANDS } from "$lib/contracts/library/chapter.commands";
import { LIBRARY_EVENTS } from "$lib/contracts/library/chapter.events";
import type { ChapterDto, ChapterFileDto } from "$lib/contracts/library/chapter.payloads";
import type { ErrorPayload } from "$lib/contracts/shared/shared.payloads";
import { resolveErrorMessage } from "$lib/contracts/errors/errors.i18n";
import { notificationStore } from "$lib/components/acerola-notification/acerola-notification.svelte";
import { LRUService } from "$lib/services/lru.service";
import { onMount } from "svelte";

const { notify } = notificationStore;

/**
 * Production-ready hook for managing comic chapters with a sliding window LRU cache.
 * Implements a continuous memory window to prevent list fragmentation.
 */
export function useComicChapters() {
  // Configured for 12 pages (~300 items) to keep RAM usage low
  const lruCache = new LRUService<number, ChapterFileDto[]>({ max: 12 });

  let cacheVersion = $state(0);
  let loading = $state(false);
  
  // Metadata stores top-level DTO info, items are handled by LRU
  let metadata = $state<Omit<ChapterDto, "archive"> & { archive: Omit<ChapterDto["archive"], "items"> } | undefined>(undefined);

  /**
   * Clears all cached data and resets the state.
   */
  function clear() {
    lruCache.clear();
    metadata = undefined;
    cacheVersion++;
    loading = false;
  }

  /**
   * Marks a page as recently used to prevent its eviction from the LRU.
   */
  function touch(pageIndex: number) {
    const hasPage = lruCache.has(pageIndex);
    if (hasPage) {
      lruCache.get(pageIndex); // Updates LRU order
      cacheVersion++;
    }
  }

  /**
   * Sets up a permanent event listener for chapter data from the backend.
   */
  onMount(() => {
    let unlistenChapters: (() => void) | undefined;
    let unlistenError: (() => void) | undefined;

    const setupListeners = async () => {
      unlistenChapters = await listen<ChapterDto>(
        LIBRARY_EVENTS.comicChapters,
        (event) => {
          const payload = event.payload;
          
          // Continuous Window Enforcement:
          // We only allow the cache to grow if the new page is a direct neighbor.
          // This prevents gaps that break the physical scroll logic.
          const cachedPageIndices = lruCache.keys;
          if (cachedPageIndices.length > 0) {
              const currentMin = Math.min(...cachedPageIndices);
              const currentMax = Math.max(...cachedPageIndices);
              const isAdjacent = payload.archive.page >= currentMin - 1 && payload.archive.page <= currentMax + 1;
              
              if (!isAdjacent) {
                  console.log(`[useComicChapters] Window continuity broken (Received: ${payload.archive.page}, Window: ${currentMin}-${currentMax}). Resetting to page ${payload.archive.page}`);
                  lruCache.clear();
              }
          }

          lruCache.set(payload.archive.page, payload.archive.items);
          console.log(`[useComicChapters] SET page ${payload.archive.page}. Cache window: ${Math.min(...lruCache.keys)}-${Math.max(...lruCache.keys)}`);
          
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
        }
      );

      unlistenError = await listen<ErrorPayload>(
        LIBRARY_EVENTS.comicChaptersError,
        (event) => {
          const errorMessage = resolveErrorMessage(event.payload);
          notify.error("Erro ao carregar capítulos", { description: errorMessage });
          toast.error(errorMessage);
          loading = false;
        }
      );
    };

    setupListeners();

    return () => {
      unlistenChapters?.();
      unlistenError?.();
    };
  });

  /**
   * Fetches a specific page of chapters from the backend.
   */
  async function fetch(
    comicDirectoryId: string,
    pageIndex: number,
    pageSize: number,
    isAscending: boolean,
    volumeId: string | null = null
  ) {
    const isAlreadyCached = lruCache.has(pageIndex);
    if (loading || isAlreadyCached) return;

    // Boundary check
    const totalItems = metadata?.archive.total ?? 0;
    const isAtEnd = metadata && pageIndex * pageSize >= totalItems && pageIndex > 0;
    if (isAtEnd) return;

    loading = true;

    try {
      await invoke(LIBRARY_COMMANDS.getComicChapters, {
        comicDirectoryFk: comicDirectoryId,
        volumeId,
        page: pageIndex,
        pageSize,
        asc: isAscending,
      });
    } catch (error) {
      console.error("[useComicChapters] Backend invoke failed:", error);
      loading = false;
    }
  }

  // Derived state reconstructs the ChapterDto from LRU and Metadata
  const chapters = $derived.by(() => {
    cacheVersion; // Re-calculate when LRU changes
    
    if (!metadata) return undefined;

    const allSortedItems = lruCache.keys
      .sort((indexA, indexB) => indexA - indexB)
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
    get chapters() { return chapters; },
    get loading() { return loading; },
    get lruKeys() { return lruCache.keys; }
  };
}
