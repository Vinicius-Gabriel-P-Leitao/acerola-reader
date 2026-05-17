<script lang="ts">
  import { useComicContext } from "$lib/state/comic-context.svelte";
  import { convertFileSrc } from "@tauri-apps/api/core";
  import { fade } from "svelte/transition";
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";
  import AcerolaToggleGroup from "$lib/components/acerola-toggle-group/acerola-toggle-group.svelte";
  import { ToggleGroupItem } from "$lib/components/ui/toggle-group/index.js";
  import ArrowLeft from "@lucide/svelte/icons/arrow-left";

  import ComicMetadataPanel from "./components/comic-metadata-panel.svelte";
  import ComicHeroBanner from "./components/comic-hero-banner.svelte";
  import ComicChapterList from "./components/comic-chapter-list.svelte";
  import ComicVolumeList from "./components/comic-volume-list.svelte";
  import ComicPreferences from "./components/comic-preferences.svelte";

  let { data } = $props();

  let activeTab = $state("chapters");
  let displayMode = $state("Lista");
  let mediaType = $state("Manga");
  let chaptersPerPage = $state("100");

  const onBack = () => {
    window.history.back();
  };

  const activeComic = useComicContext();

  const resolveBanner = (bannerPath?: string | null) =>
    bannerPath
      ? convertFileSrc(bannerPath.replaceAll("\\", "/"))
      : "https://placehold.co/1200x400/1e1e2e/a6accd?text=Banner";

  // Mapeamento limpo do objeto do banco (Rust) para a UI
  const manga = $derived.by(() => {
    const item = activeComic.item;

    return {
      id: item?.relations.directoryId.toString() || "1",
      title:
        item?.metadata.title ||
        item?.filesystem.folderName ||
        data.folderName ||
        "Manga Title",
      author: "Autor Desconhecido",
      category: "Manga",
      chaptersCount: item?.metadata.chapterCount || 0,
      description: "Uma jornada épica espera por você.",
      cover:
        activeComic.coverUrl ||
        "https://placehold.co/400x600/2a2a35/a6accd?text=Cover",
      banner: resolveBanner(item?.artwork.banner),
      chapters: [
      {
        id: "c1",
        title: "Capítulo 1: O Início",
        date: "12 Out 2023",
        fileName: "001.cbz",
        isRead: true,
      },
      {
        id: "c2",
        title: "Capítulo 2: A Descoberta",
        date: "19 Out 2023",
        fileName: "002.cbz",
        isRead: false,
      },
      {
        id: "c3",
        title: "Capítulo 3: O Desafio",
        date: "26 Out 2023",
        fileName: "003.cbz",
        isRead: false,
      },
    ],
    volumes: [
      {
        id: "v1",
        title: "Volume 1",
        chapters: [
          {
            id: "c1",
            title: "Capítulo 1: O Início",
            date: "12 Out 2023",
            isRead: true,
          },
          {
            id: "c2",
            title: "Capítulo 2: A Descoberta",
            date: "19 Out 2023",
            isRead: false,
          },
        ],
      },
      {
        id: "v2",
        title: "Volume 2",
        chapters: [
          {
            id: "c3",
            title: "Capítulo 3: O Desafio",
            date: "26 Out 2023",
            isRead: false,
          },
        ],
      },
    ],
    };
  });
</script>

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
    author={manga.author}
    category={manga.category}
    chaptersCount={manga.chaptersCount}
    description={manga.description}
    cover={manga.cover}
    onBack={onBack}
  />

  <!-- MAIN CONTENT AREA: SCROLLABLE -->
  <div class="flex-1 overflow-y-auto scrollbar-hide relative z-10 flex flex-col">
    <!-- MOBILE HEADER -->
    <div class="lg:hidden sticky top-0 z-50 flex items-center justify-between px-6 h-20 bg-base/90 backdrop-blur-md border-b border-surface/30">
      <AcerolaButtonIcon onclick={onBack} size="sm">
        <ArrowLeft size={20} />
      </AcerolaButtonIcon>
      <span class="font-black text-sm uppercase tracking-widest truncate max-w-50">{manga.title}</span>
      <div class="w-10"></div>
    </div>

    <!-- HERO BANNER SECTION -->
    <ComicHeroBanner banner={manga.banner} />

    <div class="max-w-5xl w-full mx-auto p-8 lg:p-16 space-y-12">
      <!-- TAB NAVIGATION -->
      <div class="sticky top-0 z-40 bg-base/5 backdrop-blur-3xl border-b border-surface/30 px-4 -mx-4">
        <AcerolaToggleGroup type="single" bind:value={activeTab} class="flex gap-4">
          {#snippet children()}
            <ToggleGroupItem value="chapters" class="py-6 bg-transparent border-none text-sm font-black uppercase tracking-[0.2em] relative data-[state=on]:text-primary data-[state=on]:bg-transparent">
              Capítulos
              {#if activeTab === "chapters"}
                <div class="absolute bottom-0 left-0 right-0 h-1 bg-primary rounded-full" in:fade></div>
              {/if}
            </ToggleGroupItem>
            <ToggleGroupItem value="volumes" class="py-6 bg-transparent border-none text-sm font-black uppercase tracking-[0.2em] relative data-[state=on]:text-primary data-[state=on]:bg-transparent">
              Volumes
              {#if activeTab === "volumes"}
                <div class="absolute bottom-0 left-0 right-0 h-1 bg-primary rounded-full" in:fade></div>
              {/if}
            </ToggleGroupItem>
            <ToggleGroupItem value="preferences" class="py-6 bg-transparent border-none text-sm font-black uppercase tracking-[0.2em] relative data-[state=on]:text-primary data-[state=on]:bg-transparent">
              Preferências
              {#if activeTab === "preferences"}
                <div class="absolute bottom-0 left-0 right-0 h-1 bg-primary rounded-full" in:fade></div>
              {/if}
            </ToggleGroupItem>
          {/snippet}
        </AcerolaToggleGroup>
      </div>

      <div class="min-h-150">
        {#if activeTab === "chapters"}
          <ComicChapterList chapters={manga.chapters} />
        {:else if activeTab === "volumes"}
          <ComicVolumeList volumes={manga.volumes} />
        {:else if activeTab === "preferences"}
          <ComicPreferences bind:displayMode bind:chaptersPerPage bind:mediaType />
        {/if}
      </div>
    </div>
  </div>
</div>
