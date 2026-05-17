<script lang="ts">
  import { useComicContext } from "$lib/state/comic-context.svelte";
  import { resolveBanner, resolveCover } from "$lib/utils/artwork.utils";
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

  let chaptersPerPage = $state("100");
  let activeTab = $state("chapters");
  let displayMode = $state("Lista");
  let mediaType = $state("Manga");
  let searchQuery = $state("");

  const onBack = () => {
    window.history.back();
  };

  const activeComic = useComicContext();

  // Mapeamento limpo do objeto do banco (Rust) para a UI
  const manga = $derived.by(() => {
    const item = activeComic.item;
    const artwork = item?.artwork ?? {};

    const allChapters = Array.from({ length: 50 }, (_, i) => ({
      id: `c${i + 1}`,
      title: `Capítulo ${i + 1}: ${i === 0 ? "O Início" : i === 49 ? "O Fim" : "Continuação"}`,
      date: `${10 + (i % 20)} Out 2023`,
      fileName: `${(i + 1).toString().padStart(3, "0")}.cbz`,
      isRead: i < 5,
    }));

    const filteredChapters = allChapters.filter((chapter) =>
      chapter.title.toLowerCase().includes(searchQuery.toLowerCase())
    );

    return {
      id: item?.relations.directoryId.toString() || "1",
      title:
        item?.metadata.title ||
        item?.filesystem.folderName ||
        data.folderName ||
        "Manga Title",
      chaptersCount: item?.metadata.chapterCount || allChapters.length,
      cover: resolveCover(artwork),
      banner: resolveBanner(artwork),
      description: "Uma jornada épica espera por você. Guts, o Espadachim Negro, busca vingança contra Griffith e a Mão de Deus em um mundo sombrio repleto de demônios e horror indescritível.",
      author: "Autor Desconhecido",
      category: "Manga",
      status: "Em lançamento",
      source: "MANGADEX",
      genres: [
        "Award Winning", "Monsters", "Action", "Demons", "Psychological", 
        "Seinen", "Dark Fantasy", "Adventure", "Gore", "Military", 
        "Supernatural", "Tragedy", "Mystery", "Philosophy", "Epic"
      ],
      chapters: filteredChapters,
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
    status={manga.status}
    source={manga.source}
    chaptersCount={manga.chaptersCount}
    description={manga.description}
    cover={manga.cover}
    {onBack}
  />

  <!-- MAIN CONTENT AREA: SCROLLABLE -->
  <div
    class="flex-1 overflow-y-auto scrollbar-hide relative z-10 flex flex-col"
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
    <ComicHeroBanner banner={manga.banner} genres={manga.genres} />

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
              value="chapters"
              class="py-6 bg-transparent border-none text-sm font-black uppercase tracking-[0.2em] relative data-[state=on]:text-primary data-[state=on]:bg-transparent"
            >
              Capítulos
              {#if activeTab === "chapters"}
                <div
                  class="absolute bottom-0 left-0 right-0 h-1 bg-primary rounded-full"
                  in:fade
                ></div>
              {/if}
            </ToggleGroupItem>

            <ToggleGroupItem
              value="volumes"
              class="py-6 bg-transparent border-none text-sm font-black uppercase tracking-[0.2em] relative data-[state=on]:text-primary data-[state=on]:bg-transparent"
            >
              Volumes
              {#if activeTab === "volumes"}
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

        {#if activeTab === "chapters"}
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
        {#if activeTab === "chapters"}
          <ComicChapterList chapters={manga.chapters} />
        {:else if activeTab === "volumes"}
          <ComicVolumeList volumes={manga.volumes} />
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
