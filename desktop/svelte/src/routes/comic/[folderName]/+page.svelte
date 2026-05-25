<script lang="ts">
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";
  import AcerolaToggleGroup from "$lib/components/acerola-toggle-group/acerola-toggle-group.svelte";
  import { ToggleGroupItem } from "$lib/components/ui/toggle-group/index.js";
  import { STORE_FILE, STORE_KEYS } from "$lib/constants/store-plugin";
  import { useComicChapters } from "$lib/hooks/store/use-comic-chapters.svelte";
  import { useComicContext } from "$lib/state/comic-context.svelte";
  import { resolveBanner, resolveCover } from "$lib/utils/artwork.utils";
  import ArrowLeft from "@lucide/svelte/icons/arrow-left";
  import { load } from "@tauri-apps/plugin-store";
  import { onMount, untrack } from "svelte";
  import { fade } from "svelte/transition";

  import RefreshCw from "@lucide/svelte/icons/refresh-cw";
  import ComicChapterList from "./components/comic-chapter-list.svelte";
  import ComicHeroBanner from "./components/comic-hero-banner.svelte";
  import ComicMetadataPanel from "./components/comic-metadata-panel.svelte";
  import ComicPreferences from "./components/comic-preferences.svelte";
  import ComicVolumeList from "./components/comic-volume-list.svelte";

  let { data } = $props();

  let expandedVolumeId = $state<string | null>(null);
  let chaptersPerPage = $state("25");
  let activeTab = $state("content");
  let displayMode = $state("Lista");
  let mediaType = $state("Manga");
  let isAscending = $state(true);
  let searchQuery = $state("");

  const onBack = () => window.history.back();

  const activeComic = useComicContext();
  const chapterStore = useComicChapters();

  $effect(() => {
    if (data.initialChaptersPerPage)
      chaptersPerPage = data.initialChaptersPerPage;
  });

  onMount(() => {
    if (!activeComic.item && data.comic) {
      activeComic.set(data.comic, resolveCover(data.comic.artwork));
    }
  });

  $effect(() => {
    const vol = expandedVolumeId;
    const comic = activeComic.item ?? data.comic;
    untrack(() => {
      visiblePages = [];

      // Se apenas mudamos de volume no mesmo mangá, mantemos metadados para não dar flicker
      chapterStore.clear(true);
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

  $effect(() => {
    const savePreference = async () => {
      const store = await load(STORE_FILE);

      await store.set(STORE_KEYS.chaptersPerPage, chaptersPerPage);
      await store.save();
    };
    savePreference();
  });

  let visiblePages = $state<number[]>([]);

  $effect(() => {
    if (chapterStore.loading || visiblePages.length === 0) return;

    const comic = activeComic.item ?? data.comic;
    if (!comic) return;

    for (const page of visiblePages) {
      if (chapterStore.lruKeys.includes(page)) chapterStore.touch(page);
    }

    const missingPages = visiblePages.filter(
      (page) => !chapterStore.lruKeys.includes(page),
    );

    if (missingPages.length > 0) {
      untrack(() => {
        chapterStore.fetch(
          comic.relations.directoryId,
          missingPages[0],
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

    const pageSize = parseInt(chaptersPerPage);
    const chaptersData = chapterStore.chapters;
    const totalItems = chaptersData?.archive.total ?? 0;

    const pagesData = (chaptersData?.pages ?? []).map((it) => ({
      page: it.page,
      items: it.items
        .filter((comic) => {
          // prettier-ignore
          const matchesSearch = comic.name.toLowerCase().includes(searchQuery.toLowerCase());
          // prettier-ignore
          const matchesVolume = !expandedVolumeId || comic.volumeId === expandedVolumeId;

          return matchesSearch && matchesVolume;
        })
        .map((comic) => ({
          id: comic.id.toString(),
          title: comic.name,
          fileName: comic.name,
          isRead: false, // TODO: Isso vai vim do historico
          chapterSort: comic.chapterSort,
          volumeName: comic.volumeName,
        })),
    }));

    // Mantemos volumes com info básica, mas chapters reais virão de pagesData filtrado por volume
    const volumes = (chaptersData?.archive.volumes ?? []).map((volume) => ({
      id: volume.id.toString(),
      title: volume.name,
      totalChapters: volume.chapterCount,
      hasMore:
        expandedVolumeId === volume.id.toString() &&
        pagesData.flatMap((page) => page.items).length < volume.chapterCount,
      chapters: [],
    }));

    return {
      id: item.relations.directoryId.toString(),
      chaptersCount: totalItems,
      title: item.metadata.title || item.filesystem.folderName,
      cover: resolveCover(item.artwork),
      banner: resolveBanner(item.artwork),
      pagesData,
      volumes,
      pageSize,
      metadata: {
        description: "Descrição indisponível.",
        author: "Autor Desconhecido",
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
        alt={manga.title}
        src={manga.banner || manga.cover}
        referrerpolicy="no-referrer"
        class="w-full h-full object-cover scale-150 blur-[120px] opacity-20"
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
        <span
          class="font-black text-sm uppercase tracking-widest truncate max-w-50"
        >
          {manga.title}
        </span>

        <div class="w-10"></div>
      </div>

      <ComicHeroBanner banner={manga.banner} genres={manga.metadata.genres} />

      <div class="max-w-5xl w-full mx-auto p-8 lg:p-16 space-y-12">
        <div
          class="sticky top-0 z-40 bg-base/5 backdrop-blur-3xl border-b border-surface/30 px-4 -mx-4 flex items-center justify-between"
        >
          <AcerolaToggleGroup
            type="single"
            class="flex gap-4"
            bind:value={activeTab}
          >
            {#snippet children()}
              <ToggleGroupItem
                value="content"
                class="py-6 bg-transparent border-none text-sm font-black uppercase tracking-[0.2em] relative data-[state=on]:text-primary data-[state=on]:bg-transparent"
              >
                {chapterStore.chapters?.hasVolumeStructure
                  ? "Volumes"
                  : "Capítulos"}
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
                pagesData={manga.pagesData}
                loading={chapterStore.loading}
                pageSize={manga.pageSize}
                onexpand={(v) => (expandedVolumeId = v)}
                onvisiblepages={(p) => (visiblePages = p)}
              />
            {:else}
              <ComicChapterList
                pagesData={manga.pagesData}
                totalChapters={manga.chaptersCount}
                pageSize={manga.pageSize}
                onvisiblepages={(p) => (visiblePages = p)}
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
