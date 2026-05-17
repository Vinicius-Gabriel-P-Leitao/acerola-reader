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
  let activeTab = $state("chapters");
  let displayMode = $state("Lista");
  let mediaType = $state("Manga");
  let searchQuery = $state("");

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
    if (!activeComic.item && data.comic) {
      activeComic.set(data.comic, resolveCover(data.comic.artwork));
    }
  });

  $effect(() => {
    // Captura as dependências que devem disparar a busca
    const comicItem = activeComic.item ?? data.comic;
    const page = currentPage;
    const size = parseInt(chaptersPerPage);
    const asc = isAscending;

    if (comicItem) {
      // Usamos untrack para chamar o fetch sem rastrear estados internos do store (ex: loading)
      untrack(() => {
        chapterStore.fetch(
          comicItem.relations.directoryId,
          page,
          size,
          asc,
        );
      });
    } else {
      untrack(() => {
        const errorMessage = "Quadrinho não encontrado ou removido.";
        notify.error("Erro de Carregamento", {
          description: errorMessage,
        });
        toast.error(errorMessage);
        onBack();
      });
    }
  });

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

    const chapters = (chaptersData?.archive.items ?? [])
      .filter((chapterItem) =>
        chapterItem.name.toLowerCase().includes(searchQuery.toLowerCase()),
      )
      .map((chapterFile) => ({
        id: chapterFile.id.toString(),
        title: chapterFile.name, // Será formatado no componente
        fileName: chapterFile.name,
        isRead: false, // Histórico ainda não implementado
        chapterSort: chapterFile.chapterSort,
        volumeName: chapterFile.volumeName,
      }));

    const volumes = (chaptersData?.archive.volumeSections ?? []).map(
      (section) => ({
        id: section.volume.id.toString(),
        title: section.volume.name,
        cover: resolveCover({
          cover: section.volume.coverUri,
          banner: section.volume.bannerUri,
        }),
        chapters: section.items.map((chapterFile) => ({
          id: chapterFile.id.toString(),
          title: chapterFile.name,
          isRead: false,
        })),
      }),
    );

    return {
      id: item.relations.directoryId.toString(),
      title: item.metadata.title || item.filesystem.folderName,
      chaptersCount:
        item.metadata.chapterCount || chaptersData?.archive.total || 0,
      cover: resolveCover(artwork),
      banner: resolveBanner(artwork),
      chapters,
      volumes,
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
{:else}
  <div class="flex items-center justify-center h-screen bg-base text-overlay">
    <RefreshCw size={48} class="animate-spin text-primary" />
  </div>
{/if}
