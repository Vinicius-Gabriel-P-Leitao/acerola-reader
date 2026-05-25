<!-- VERSION: 6.0 - INTERSECTION OBSERVER BASED SCROLL DETECTION -->
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

  // 1. Sync Preferences
  $effect(() => {
    if (data.initialChaptersPerPage) {
      chaptersPerPage = data.initialChaptersPerPage;
    }
  });

  onMount(() => {
    if (!activeComic.item && data.comic) {
      activeComic.set(data.comic, resolveCover(data.comic.artwork));
    }
  });

  // 2. Navigation Reset — reseta e busca página 0 quando abrimos uma nova guia
  $effect(() => {
    const tab = activeTab;
    const vol = expandedVolumeId;
    const comic = activeComic.item ?? data.comic;

    untrack(() => {
      console.log(`[ComicPage] Navigation Reset: Tab=${tab}, Vol=${vol}`);
      chapterStore.clear();

      if (comic) {
        chapterStore.fetch(
          comic.relations.directoryId,
          0,
          parseInt(chaptersPerPage),
          isAscending,
          vol,
        );
      }
    });
  });

  // 3. Persistent Preferences
  $effect(() => {
    const savePreference = async () => {
      const store = await load(STORE_FILE);
      await store.set(STORE_KEYS.chaptersPerPage, chaptersPerPage);
      await store.save();
    };
    savePreference();
  });

  // 4. LRU & Virtualization gerido por Interseções
  let visiblePages = $state<number[]>([]);

  $effect(() => {
    if (chapterStore.loading) return;
    if (visiblePages.length === 0) return;

    const comic = activeComic.item ?? data.comic;
    if (!comic) return;

    // Primeiro mantemos a recência no LRU para as páginas que ainda estão visíveis
    for (const page of visiblePages) {
      if (chapterStore.lruKeys.includes(page)) {
        chapterStore.touch(page);
      }
    }

    // Identificamos a primeira página visível que nosso LRU não possui e pedimos ela
    const missingPages = visiblePages.filter(
      (p) => !chapterStore.lruKeys.includes(p)
    );

    if (missingPages.length > 0) {
      const pageToFetch = missingPages[0];
      
      untrack(() => {
        chapterStore.fetch(
          comic.relations.directoryId,
          pageToFetch,
          parseInt(chaptersPerPage),
          isAscending,
          expandedVolumeId,
        );
      });
    }
  });

  const manga = $derived.by(() => {
    const item = activeComic.item ?? data.comic;
    if (!item) return null;

    const chaptersData = chapterStore.chapters;
    const lruKeys = chapterStore.lruKeys;
    const pageSize = parseInt(chaptersPerPage);
    const totalItems = chaptersData?.archive.total ?? 0;

    const minPage = lruKeys.length > 0 ? Math.min(...lruKeys) : 0;

    const chapters = (chaptersData?.archive.items ?? [])
      .filter((c) => {
        const matchesSearch = c.name
          .toLowerCase()
          .includes(searchQuery.toLowerCase());
        const matchesVolume =
          !expandedVolumeId || c.volumeId === expandedVolumeId;
        return matchesSearch && matchesVolume;
      })
      .map((c) => ({
        id: c.id.toString(),
        title: c.name.replace(/\.(cbz|cbr|zip|rar)$/i, ""),
        fileName: c.name,
        isRead: false,
        chapterSort: c.chapterSort,
        volumeName: c.volumeName,
      }));

    const volumes = (chaptersData?.archive.volumes ?? []).map((v) => ({
      id: v.id.toString(),
      title: v.name,
      totalChapters: v.chapterCount,
      hasMore:
        expandedVolumeId === v.id.toString() &&
        chapters.length < v.chapterCount,
      chapters: expandedVolumeId === v.id.toString() ? chapters : [],
    }));

    return {
      id: item.relations.directoryId.toString(),
      title: item.metadata.title || item.filesystem.folderName,
      chaptersCount: totalItems,
      cover: resolveCover(item.artwork),
      banner: resolveBanner(item.artwork),
      chapters,
      volumes,
      minPage,
      pageSize,
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
    <div class="absolute inset-0 overflow-hidden pointer-events-none">
      <img
        src={manga.banner || manga.cover}
        alt=""
        class="w-full h-full object-cover scale-150 blur-[120px] opacity-20"
        referrerpolicy="no-referrer"
      />
      <div class="absolute inset-0 bg-base/40"></div>
    </div>

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

    <div
      class="flex-1 overflow-y-auto scrollbar-hide relative z-10 flex flex-col [overflow-anchor:none]"
    >
      <div
        class="lg:hidden sticky top-0 z-50 flex items-center justify-between px-6 h-20 bg-base/90 backdrop-blur-md border-b border-surface/30"
      >
        <AcerolaButtonIcon onclick={onBack} size="sm">
          <ArrowLeft size={20} />
        </AcerolaButtonIcon>
        <span class="font-black text-sm uppercase tracking-widest truncate max-w-50"
          >{manga.title}</span
        >
        <div class="w-10"></div>
      </div>

      <ComicHeroBanner banner={manga.banner} genres={manga.metadata.genres} />

      <div class="max-w-5xl w-full mx-auto p-8 lg:p-16 space-y-12">
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
                {chapterStore.chapters?.hasVolumeStructure
                  ? "Volumes"
                  : "Capítulos"}
                {#if activeTab === "content"}<div
                    class="absolute bottom-0 left-0 right-0 h-1 bg-primary rounded-full"
                    in:fade
                  ></div>{/if}
              </ToggleGroupItem>
              <ToggleGroupItem
                value="preferences"
                class="py-6 bg-transparent border-none text-sm font-black uppercase tracking-[0.2em] relative data-[state=on]:text-primary data-[state=on]:bg-transparent"
              >
                Preferências
                {#if activeTab === "preferences"}<div
                    class="absolute bottom-0 left-0 right-0 h-1 bg-primary rounded-full"
                    in:fade
                  ></div>{/if}
              </ToggleGroupItem>
            {/snippet}
          </AcerolaToggleGroup>

          {#if activeTab === "content" && !chapterStore.chapters?.hasVolumeStructure}
            <div class="flex items-center gap-2 pr-4">
              <input
                type="text"
                bind:value={searchQuery}
                placeholder="FILTRAR..."
                class="bg-surface/20 border border-surface/30 rounded-full py-2 px-6 text-[10px] font-black tracking-widest focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all w-40 focus:w-60"
              />
            </div>
          {/if}
        </div>

        <div class="min-h-150">
          {#if activeTab === "content"}
            {#if chapterStore.chapters?.hasVolumeStructure}
              <ComicVolumeList
                volumes={manga.volumes}
                loading={chapterStore.loading}
                minPage={manga.minPage}
                pageSize={manga.pageSize}
                onexpand={(volumeId) => (expandedVolumeId = volumeId)}
                onvisiblepages={(pages) => {
                  visiblePages = pages;
                }}
              />
            {:else}
              <ComicChapterList
                chapters={manga.chapters}
                totalChapters={manga.chaptersCount}
                minPage={manga.minPage}
                pageSize={manga.pageSize}
                onvisiblepages={(pages) => {
                  visiblePages = pages;
                }}
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
