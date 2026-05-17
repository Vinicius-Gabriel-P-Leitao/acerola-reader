<script lang="ts">
  import { useComicContext } from "$lib/state/comic-context.svelte";
  import { convertFileSrc } from "@tauri-apps/api/core";
  import { fade, slide, fly } from "svelte/transition";
  import AcerolaButton from "$lib/components/acerola-button/acerola-button.svelte";
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";
  import AcerolaHeroButton from "$lib/components/acerola-hero-button/acerola-hero-button.svelte";
  import AcerolaSelect from "$lib/components/acerola-select/acerola-select.svelte";
  import AcerolaToggleGroup from "$lib/components/acerola-toggle-group/acerola-toggle-group.svelte";
  import { ToggleGroupItem } from "$lib/components/ui/toggle-group/index.js";

  import ArrowLeft from "@lucide/svelte/icons/arrow-left";
  import Play from "@lucide/svelte/icons/play";
  import Bookmark from "@lucide/svelte/icons/bookmark";
  import RefreshCw from "@lucide/svelte/icons/refresh-cw";
  import BookOpen from "@lucide/svelte/icons/book-open";
  import ChevronDown from "@lucide/svelte/icons/chevron-down";
  import Folder from "@lucide/svelte/icons/folder";
  import FileText from "@lucide/svelte/icons/file-text";
  import MoreVertical from "@lucide/svelte/icons/more-vertical";
  import Settings2 from "@lucide/svelte/icons/settings-2";
  import Tag from "@lucide/svelte/icons/tag";
  import List from "@lucide/svelte/icons/list";
  import Star from "@lucide/svelte/icons/star";

  let { data } = $props();

  let activeTab = $state("chapters");
  let expandedVolumes = $state<string[]>([]);
  let displayMode = $state("Lista");
  let mediaType = $state("Manga");
  let chaptersPerPage = $state("100");

  const toggleVolume = (volumeId: string) => {
    if (expandedVolumes.includes(volumeId)) {
      expandedVolumes = expandedVolumes.filter((id) => id !== volumeId);
    } else {
      expandedVolumes = [...expandedVolumes, volumeId];
    }
  };

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
  <div
    class="hidden lg:flex w-100 flex-col h-full bg-mantle/60 border-r border-surface/30 backdrop-blur-3xl relative z-10 shrink-0 select-none"
  >
    <div class="p-10 flex flex-col h-full">
      <AcerolaButtonIcon onclick={onBack} class="mb-10 shadow-lg group">
        <ArrowLeft
          size={24}
          class="group-hover:-translate-x-1 transition-transform"
        />
      </AcerolaButtonIcon>

      <div class="flex-1 flex flex-col items-center text-center space-y-8">
        <div
          class="w-64 aspect-2/3 rounded-huge overflow-hidden shadow-2xl border-4 border-surface shrink-0"
        >
          <img
            src={manga.cover}
            alt={manga.title}
            class="w-full h-full object-cover"
            referrerpolicy="no-referrer"
          />
        </div>

        <div class="space-y-3 px-4">
          <h1 class="text-4xl font-black tracking-tighter leading-tight">
            {manga.title}
          </h1>
          <p class="text-lg text-primary font-bold">{manga.author}</p>
          <div class="flex flex-wrap justify-center gap-2 pt-2">
            <span
              class="px-3 py-1 bg-surface/50 rounded-lg text-[10px] font-black uppercase tracking-widest text-subtext"
              >{manga.category}</span
            >
            <span
              class="px-3 py-1 bg-surface/50 rounded-lg text-[10px] font-black uppercase tracking-widest text-subtext"
              >{manga.chaptersCount} Caps</span
            >
          </div>
        </div>

        <div
          class="w-full bg-base/30 border border-surface/30 rounded-3xl p-6 text-left space-y-3"
        >
          <h3
            class="text-[10px] font-black uppercase tracking-widest text-overlay"
          >
            Sinopse
          </h3>
          <p
            class="text-xs text-subtext leading-relaxed line-clamp-6 font-medium"
          >
            {manga.description}
          </p>
        </div>

        <div class="w-full pt-4 space-y-3">
          <AcerolaButton
            class="w-full py-8 rounded-3xl font-black flex items-center justify-center gap-3 shadow-xl shadow-primary/20"
          >
            <Play size={24} fill="currentColor" />
            LER AGORA
          </AcerolaButton>
          <div class="grid grid-cols-2 gap-3">
            <AcerolaButton
              variant="outline"
              class="py-6 rounded-2xl font-black flex items-center justify-center gap-2 text-xs"
            >
              <Bookmark size={16} /> SALVAR
            </AcerolaButton>
            <AcerolaButton
              variant="outline"
              class="py-6 rounded-2xl font-black flex items-center justify-center gap-2 text-xs"
            >
              <RefreshCw size={16} /> SYNC
            </AcerolaButton>
          </div>
        </div>
      </div>
    </div>
  </div>

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
    <div class="relative w-full h-75 lg:h-112.5 shrink-0">
      {#if manga.banner}
        <div class="w-full h-full relative">
          <img
            src={manga.banner}
            alt="Banner"
            class="w-full h-full object-cover"
            referrerpolicy="no-referrer"
          />
          <div
            class="absolute inset-0 bg-linear-to-t from-base via-base/20 to-transparent"
          ></div>
          <div
            class="absolute inset-0 bg-linear-to-l from-transparent via-transparent to-base/40 hidden lg:block"
          ></div>
        </div>
      {:else}
        <div
          class="w-full h-full bg-linear-to-br from-primary/20 via-surface to-base flex items-center justify-center"
        >
          <div class="opacity-10 scale-[5]"><BookOpen size={100} /></div>
        </div>
      {/if}

      <!-- Floating stats on Banner -->
      <div class="absolute bottom-10 left-10 lg:left-16 flex gap-6">
        <div
          class="flex items-center gap-3 bg-surface/20 backdrop-blur-xl border border-surface/30 px-6 py-3 rounded-2xl"
        >
          <Star size={20} class="text-yellow-400 fill-yellow-400" />
          <span class="font-black text-xl tracking-tighter">9.8</span>
        </div>
        <div
          class="flex items-center gap-3 bg-surface/20 backdrop-blur-xl border border-surface/30 px-6 py-3 rounded-2xl"
        >
          <List size={20} class="text-primary" />
          <span class="font-black text-xl tracking-tighter">Popular</span>
        </div>
      </div>
    </div>

    <div class="max-w-5xl w-full mx-auto p-8 lg:p-16 space-y-12">
      <!-- TAB NAVIGATION -->
      <div
        class="sticky top-0 z-40 bg-base/5 backdrop-blur-3xl border-b border-surface/30 px-4 -mx-4"
      >
        <AcerolaToggleGroup type="single" bind:value={activeTab} class="flex gap-4">
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
      </div>

      <div class="min-h-150">
        {#if activeTab === "chapters"}
          <div class="space-y-3">
            {#if manga.chapters && manga.chapters.length > 0}
              {#each manga.chapters as chapter, i}
                <div in:fly={{ y: 10, delay: i * 20, duration: 200 }}>
                  <AcerolaHeroButton
                    title={chapter.title}
                    description="{chapter.date} • {chapter.fileName}"
                    class="bg-mantle/40 border-surface/40 hover:bg-surface/30"
                  >
                    {#snippet icon()}
                      <div
                        class={chapter.isRead ? "text-overlay" : "text-primary"}
                      >
                        <BookOpen size={24} />
                      </div>
                    {/snippet}
                    {#snippet action()}
                      <AcerolaButtonIcon
                        variant="ghost"
                        size="sm"
                        class="text-overlay hover:text-primary"
                      >
                        <MoreVertical size={20} />
                      </AcerolaButtonIcon>
                    {/snippet}
                  </AcerolaHeroButton>
                </div>
              {/each}
            {:else}
              <div class="py-20 text-center opacity-50 space-y-4">
                <RefreshCw
                  size={48}
                  class="animate-spin mx-auto text-primary"
                />
                <p class="font-black uppercase tracking-widest text-sm">
                  Sincronizando capítulos...
                </p>
              </div>
            {/if}
          </div>
        {:else if activeTab === "volumes"}
          <div class="space-y-4">
            {#if manga.volumes && manga.volumes.length > 0}
              {#each manga.volumes as volume}
                <AcerolaHeroButton
                  title={volume.title}
                  description="{volume.chapters.length} Capítulos inclusos"
                  onclick={() => toggleVolume(volume.id)}
                  class="bg-mantle/40 border-surface/40"
                >
                  {#snippet icon()}
                    <Folder size={28} />
                  {/snippet}
                  {#snippet action()}
                    <div
                      class="transition-transform duration-300 {expandedVolumes.includes(
                        volume.id,
                      )
                        ? 'rotate-180'
                        : ''}"
                    >
                      <ChevronDown size={20} />
                    </div>
                  {/snippet}
                </AcerolaHeroButton>

                {#if expandedVolumes.includes(volume.id)}
                  <div
                    transition:slide
                    class="space-y-2 ml-10 border-l border-surface/30 p-4 pt-0"
                  >
                    {#each volume.chapters as chapter}
                      <AcerolaHeroButton
                        title={chapter.title}
                        description={chapter.date}
                        class="bg-base/20 border-transparent hover:bg-surface/30"
                      >
                        {#snippet icon()}
                          <div
                            class="w-10 h-10 rounded-lg flex items-center justify-center {chapter.isRead
                              ? 'bg-surface/50 text-overlay'
                              : 'bg-primary/10 text-primary'}"
                          >
                            <FileText size={18} />
                          </div>
                        {/snippet}
                      </AcerolaHeroButton>
                    {/each}
                  </div>
                {/if}
              {/each}
            {:else}
              <div
                class="py-20 text-center opacity-50 space-y-4 border-2 border-dashed border-surface rounded-3xl"
              >
                <RefreshCw
                  size={48}
                  class="animate-spin mx-auto text-primary"
                />
                <p class="font-black uppercase tracking-widest text-sm">
                  Nenhum volume indexado ainda.
                </p>
              </div>
            {/if}
          </div>
        {:else if activeTab === "preferences"}
          <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
            <section class="space-y-6">
              <h3
                class="flex items-center gap-3 text-overlay uppercase text-[10px] font-black tracking-[0.3em]"
              >
                <Settings2 size={16} /> Leitura
              </h3>
              <div
                class="bg-mantle/40 border border-surface/40 rounded-4xl p-8 space-y-6"
              >
                <div class="flex items-center justify-between">
                  <span class="font-bold text-sm">Modo de Exibição</span>
                  <AcerolaToggleGroup type="single" bind:value={displayMode}>
                    {#snippet children()}
                      <ToggleGroupItem
                        value="Lista"
                        class="px-4 py-1.5 text-[10px] font-black uppercase"
                      >
                        Lista
                      </ToggleGroupItem>
                      <ToggleGroupItem
                        value="Grade"
                        class="px-4 py-1.5 text-[10px] font-black uppercase"
                      >
                        Grade
                      </ToggleGroupItem>
                    {/snippet}
                  </AcerolaToggleGroup>
                </div>
                <div class="flex items-center justify-between">
                  <span class="font-bold text-sm">Capítulos por página</span>
                  <AcerolaSelect
                    bind:value={chaptersPerPage}
                    options={[
                      { value: "25", label: "25" },
                      { value: "50", label: "50" },
                      { value: "100", label: "100" },
                    ]}
                  />
                </div>
              </div>
            </section>

            <section class="space-y-6">
              <h3
                class="flex items-center gap-3 text-overlay uppercase text-[10px] font-black tracking-[0.3em]"
              >
                <Tag size={16} /> Metadados
              </h3>
              <div
                class="bg-mantle/40 border border-surface/40 rounded-4xl p-8 space-y-6"
              >
                <div class="flex flex-col gap-4">
                  <span class="font-bold text-sm border-b border-surface pb-3"
                    >Tipo de Mídia</span>
                  <AcerolaToggleGroup
                    type="single"
                    bind:value={mediaType}
                    class="flex flex-wrap gap-2"
                  >
                    {#snippet children()}
                      {#each ["Manga", "Hq", "Novel", "Webtoon"] as cat}
                        <ToggleGroupItem
                          value={cat}
                          class="px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest"
                        >
                          {cat}
                        </ToggleGroupItem>
                      {/each}
                    {/snippet}
                  </AcerolaToggleGroup>
                </div>
              </div>
            </section>
          </div>
        {/if}
      </div>
    </div>
  </div>
</div>
