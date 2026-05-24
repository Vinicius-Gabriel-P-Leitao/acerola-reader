<script lang="ts">
  import { useComicContext } from "$lib/state/comic-context.svelte";
  import { useComicChapters } from "$lib/hooks/store/use-comic-chapters.svelte";
  import { resolveBanner, resolveCover } from "$lib/utils/artwork.utils";
  import { fade } from "svelte/transition";
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";
  import AcerolaToggleGroup from "$lib/components/acerola-toggle-group/acerola-toggle-group.svelte";
  import { ToggleGroupItem } from "$lib/components/ui/toggle-group/index.js";
  import ArrowLeft from "@lucide/svelte/icons/arrow-left";
  import { onMount, untrack } from "svelte";
  import { STORE_KEYS, STORE_FILE } from "$lib/constants/store-plugin";
  import { load } from "@tauri-apps/plugin-store";
  import { toast } from "svelte-sonner";
  import { notificationStore } from "$lib/components/acerola-notification/acerola-notification.svelte";

  import ComicMetadataPanel from "./components/comic-metadata-panel.svelte";
  import ComicHeroBanner from "./components/comic-hero-banner.svelte";
  import ComicChapterList from "./components/comic-chapter-list.svelte";
  import ComicVolumeList from "./components/comic-volume-list.svelte";
  import ComicPreferences from "./components/comic-preferences.svelte";
  import RefreshCw from "@lucide/svelte/icons/refresh-cw";

  let { data } = $props();
  const { notify } = notificationStore;

  let chaptersPerPage = $state("25");
  let currentPage = $state(0);
  let isAscending = $state(true);
  let activeTab = $state("content");
  let displayMode = $state("Lista");
  let mediaType = $state("Manga");
  let searchQuery = $state("");
  let expandedVolumeId = $state<string | null>(null);

  const onBack = () => {
    window.history.back();
  };

  const activeComic = useComicContext();
  const chapterStore = useComicChapters();

  // Sincroniza preferências iniciais vindas do loader
  $effect(() => {
    if (data.initialChaptersPerPage) {
      chaptersPerPage = data.initialChaptersPerPage;
    }
  });

  // Se o contexto estiver vazio (ex: refresh), sincroniza com os dados do loader
  onMount(() => {
    console.log("[ComicPage] Mounting, data from loader:", data);
    if (!activeComic.item && data.comic) {
      console.log("[ComicPage] Initializing context from loader data");
      activeComic.set(data.comic, resolveCover(data.comic.artwork));
    }
  });

  // Limpa o estado quando muda de aba ou volume (Exclusive expansion)
  $effect(() => {
    // Sincronizamos as chaves de dependência
    const currentTab = activeTab;
    const currentVolume = expandedVolumeId;

    untrack(() => {
      console.log(`[ComicPage] Navigation Reset (Tab: ${currentTab}, Vol: ${currentVolume})`);
      currentPage = 0;
      chapterStore.clear();
    });
  });

  // Effect centralizado para disparar a busca de dados
  $effect(() => {
    const targetComic = activeComic.item ?? data.comic;
    const targetPage = currentPage;
    const requestedSize = parseInt(chaptersPerPage);
    const orderAsc = isAscending;
    const targetVolume = expandedVolumeId;
    
    if (!targetComic) return;

    untrack(() => {
      const chaptersState = chapterStore.chapters;
      const isVolumeView = activeTab === "content" && chaptersState?.hasVolumeStructure;

      // Caso especial: Aba de volumes sem volume selecionado
      if (isVolumeView && !targetVolume) {
        console.log("[ComicPage] Fetching volume structure metadata");
        chapterStore.fetch(
          targetComic.relations.directoryId,
          0,
          1, // Apenas estrutura básica
          orderAsc,
          null
        );
        return;
      }

      console.log(`[ComicPage] Dispatching Fetch -> Page: ${targetPage}, Size: ${requestedSize}, Vol: ${targetVolume}`);
      chapterStore.fetch(
        targetComic.relations.directoryId,
        targetPage,
        requestedSize,
        orderAsc,
        targetVolume
      );
    });
  });

  let scrollContainer: HTMLElement | undefined = $state();
  let previousPaddingTop = 0;

  // Scroll Anchoring: Prevents jumping when paddingTop changes
  $effect(() => {
    const currentPaddingTop = manga?.paddingTop ?? 0;
    const paddingDelta = currentPaddingTop - previousPaddingTop;

    if (paddingDelta !== 0 && scrollContainer) {
      untrack(() => {
        // Adjust scrollTop to compensate for padding changes
        scrollContainer!.scrollTop += paddingDelta;
        console.log(`[ComicPage] Scroll Anchored: Adjusted by ${paddingDelta}px`);
      });
    }
    previousPaddingTop = currentPaddingTop;
  });

  function handleScroll(event: Event) {
    const scrollTarget = event.target as HTMLElement;
    const chaptersData = chapterStore.chapters;
    
    if (!chaptersData || chapterStore.loading) return;

    const totalItemsCount = chaptersData.archive.total;
    const itemsPerPageCount = parseInt(chaptersPerPage);
    const totalPagesCount = Math.ceil(totalItemsCount / itemsPerPageCount);
    
    // Altura física estimada de um item (80px card + 12px gap)
    const itemHeight = 92;
    const totalExpectedHeight = totalItemsCount * itemHeight;

    // 1. Telemetria Baseada na Altura Total Real (Não na do DOM atual que oscila)
    const scrollPositionPercent = scrollTarget.scrollTop / (totalExpectedHeight - scrollTarget.clientHeight);
    const focalPageIndex = Math.min(
        totalPagesCount - 1,
        Math.max(0, Math.floor(scrollPositionPercent * totalPagesCount))
    );

    if (!isNaN(focalPageIndex)) {
        chapterStore.touch(focalPageIndex);
    }

    // 2. Detecção de Bordas RELATIVA ao conteúdo carregado
    const FETCH_THRESHOLD = 1500;
    const paddingTop = manga?.paddingTop ?? 0;
    const paddingBottom = manga?.paddingBottom ?? 0;

    // Distância do topo dos ITENS carregados (não do container)
    const distanceFromContentTop = scrollTarget.scrollTop - paddingTop;
    // Distância da base dos ITENS carregados
    const distanceFromContentBottom = (scrollTarget.scrollHeight - paddingBottom) - (scrollTarget.scrollTop + scrollTarget.clientHeight);

    const cachedKeys = chapterStore.lruKeys;
    if (cachedKeys.length === 0) return;

    const minCached = Math.min(...cachedKeys);
    const maxCached = Math.max(...cachedKeys);

    // Flow Down: Chegou perto da base dos itens carregados? Pede o próximo.
    if (distanceFromContentBottom <= FETCH_THRESHOLD && maxCached < totalPagesCount - 1) {
        const nextPage = maxCached + 1;
        if (currentPage !== nextPage) {
            console.log(`[ComicPage] ⏬ Edge DOWN (Content-Relative) -> Page ${nextPage}`);
            currentPage = nextPage;
            return;
        }
    }

    // Flow Up: Chegou perto do topo dos itens carregados? Pede o anterior.
    if (distanceFromContentTop <= FETCH_THRESHOLD && minCached > 0) {
        const prevPage = minCached - 1;
        if (currentPage !== prevPage) {
            console.log(`[ComicPage] ⏫ Edge UP (Content-Relative) -> Page ${prevPage}`);
            currentPage = prevPage;
            return;
        }
    }
  }

  $effect(() => {
    const savePreference = async () => {
      const store = await load(STORE_FILE);
      await store.set(STORE_KEYS.chaptersPerPage, chaptersPerPage);
      await store.save();
    };
    savePreference();
  });

  // Mapeamento limpo do objeto do banco (Rust) para a UI
  const manga = $derived.by(() => {
    const item = activeComic.item ?? data.comic;
    if (!item) return null;

    const artwork = item.artwork ?? {};
    const chaptersData = chapterStore.chapters;
    const lruKeys = chapterStore.lruKeys;
    const pageSize = parseInt(chaptersPerPage);
    const totalItems = chaptersData?.archive.total ?? 0;

    // Cálculo de Espaçadores Virtuais (Prevenção de Loop de Scroll)
    // Assumimos 80px por item + 12px de gap (space-y-3) = ~92px
    const itemHeight = 92; 
    const minPage = lruKeys.length > 0 ? Math.min(...lruKeys) : 0;
    const maxPage = lruKeys.length > 0 ? Math.max(...lruKeys) : 0;
    
    const paddingTop = minPage * pageSize * itemHeight;
    const remainingItems = totalItems - (maxPage + 1) * pageSize;
    const paddingBottom = Math.max(0, remainingItems * itemHeight);

    const chapters = (chaptersData?.archive.items ?? [])
      .filter((chapterItem) => {
        const matchesSearch = chapterItem.name
          .toLowerCase()
          .includes(searchQuery.toLowerCase());
        const matchesVolume =
          !expandedVolumeId || chapterItem.volumeId === expandedVolumeId;
        return matchesSearch && matchesVolume;
      })
      .map((chapterFile) => {
        const cleanTitle = chapterFile.name
            .replace('.cbz', '')
            .replace('.cbr', '')
            .replace('.zip', '')
            .replace('.rar', '');

        return {
          id: chapterFile.id.toString(),
          title: cleanTitle,
          fileName: chapterFile.name,
          isRead: false,
          chapterSort: chapterFile.chapterSort,
          volumeName: chapterFile.volumeName,
        };
      });

    const volumes = (chaptersData?.archive.volumes ?? []).map(
      (volumeArchive) => ({
        id: volumeArchive.id.toString(),
        title: volumeArchive.name,
        totalChapters: volumeArchive.chapterCount,
        hasMore:
          expandedVolumeId === volumeArchive.id.toString() &&
          chapters.length < volumeArchive.chapterCount,
        chapters:
          expandedVolumeId === volumeArchive.id.toString() ? chapters : [],
      }),
    );

    return {
      id: item.relations.directoryId.toString(),
      title: item.metadata.title || item.filesystem.folderName,
      chaptersCount: totalItems,
      cover: resolveCover(artwork),
      banner: resolveBanner(artwork),
      chapters,
      volumes,
      paddingTop,
      paddingBottom,
      metadata: {
        description: "Descrição indisponível.",
        author: "Autor Desconhecido",
        category: "Manga",
        status: "Desconhecido",
        source: item.metadata.activeSource || "LOCAL",
        genres: [],
      },
    };
  });
</script>

{#if manga}
  <div
    in:fade={{ duration: 200 }}
    out:fade={{ duration: 200 }}
    class="fixed inset-0 z-50 bg-base overflow-hidden flex h-screen text-text"
  >
    <!-- Background Blur Artwork -->
    <div class="absolute inset-0 overflow-hidden pointer-events-none">
      <img
        src={manga.banner || manga.cover}
        alt=""
        class="w-full h-full object-cover scale-150 blur-[120px] opacity-20"
        referrerpolicy="no-referrer"
      />
      <div class="absolute inset-0 bg-base/40"></div>
    </div>

    <!-- LEFT PANEL: FIXED METADATA -->
    <ComicMetadataPanel
      title={manga.title}
      author={manga.metadata.author}
      status={manga.metadata.status}
      source={manga.metadata.source}
      chaptersCount={manga.chaptersCount}
      description={manga.metadata.description}
      cover={manga.cover}
      {onBack}
    />

    <!-- MAIN CONTENT AREA: SCROLLABLE -->
    <div
      bind:this={scrollContainer}
      class="flex-1 overflow-y-auto scrollbar-hide relative z-10 flex flex-col"
      onscroll={handleScroll}
    >
      <!-- MOBILE HEADER -->
      <div
        class="lg:hidden sticky top-0 z-50 flex items-center justify-between px-6 h-20 bg-base/90 backdrop-blur-md border-b border-surface/30"
      >
        <AcerolaButtonIcon onclick={onBack} size="sm">
          <ArrowLeft size={20} />
        </AcerolaButtonIcon>

        <span
          class="font-black text-sm uppercase tracking-widest truncate max-w-50"
          >{manga.title}</span
        >
        <div class="w-10"></div>
      </div>

      <!-- HERO BANNER SECTION -->
      <ComicHeroBanner banner={manga.banner} genres={manga.metadata.genres} />

      <div class="max-w-5xl w-full mx-auto p-8 lg:p-16 space-y-12">
        <!-- TAB NAVIGATION AND FILTER -->
        <div
          class="sticky top-0 z-40 bg-base/5 backdrop-blur-3xl border-b border-surface/30 px-4 -mx-4 flex items-center justify-between"
        >
          <AcerolaToggleGroup
            type="single"
            bind:value={activeTab}
            class="flex gap-4"
          >
            {#snippet children()}
              <ToggleGroupItem
                value="content"
                class="py-6 bg-transparent border-none text-sm font-black uppercase tracking-[0.2em] relative data-[state=on]:text-primary data-[state=on]:bg-transparent"
              >
                {chapterStore.chapters?.hasVolumeStructure ? 'Volumes' : 'Capítulos'}
                {#if activeTab === "content"}
                  <div
                    class="absolute bottom-0 left-0 right-0 h-1 bg-primary rounded-full"
                    in:fade
                  ></div>
                {/if}
              </ToggleGroupItem>

              <ToggleGroupItem
                value="preferences"
                class="py-6 bg-transparent border-none text-sm font-black uppercase tracking-[0.2em] relative data-[state=on]:text-primary data-[state=on]:bg-transparent"
              >
                Preferências
                {#if activeTab === "preferences"}
                  <div
                    class="absolute bottom-0 left-0 right-0 h-1 bg-primary rounded-full"
                    in:fade
                  ></div>
                {/if}
              </ToggleGroupItem>
            {/snippet}
          </AcerolaToggleGroup>

          {#if activeTab === "content" && !chapterStore.chapters?.hasVolumeStructure}
            <div class="flex items-center gap-2 pr-4">
              <div class="relative group">
                <input
                  type="text"
                  bind:value={searchQuery}
                  placeholder="FILTRAR..."
                  class="bg-surface/20 border border-surface/30 rounded-full py-2 px-6 text-[10px] font-black tracking-widest focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all w-40 focus:w-60"
                />
              </div>
            </div>
          {/if}
        </div>

        <div class="min-h-150">
          {#if activeTab === "content"}
            {#if chapterStore.chapters?.hasVolumeStructure}
                <ComicVolumeList 
                    volumes={manga.volumes} 
                    loading={chapterStore.loading}
                    onexpand={(volumeId) => expandedVolumeId = volumeId} 
                />
            {:else}
                <ComicChapterList 
                    chapters={manga.chapters} 
                    paddingTop={manga.paddingTop}
                    paddingBottom={manga.paddingBottom}
                />
            {/if}
          {:else if activeTab === "preferences"}
            <ComicPreferences
              bind:displayMode
              bind:chaptersPerPage
              bind:mediaType
            />
          {/if}
        </div>
      </div>
    </div>
  </div>
{:else}
  <div class="flex items-center justify-center h-screen bg-base text-overlay">
    <RefreshCw size={48} class="animate-spin text-primary" />
  </div>
{/if}
