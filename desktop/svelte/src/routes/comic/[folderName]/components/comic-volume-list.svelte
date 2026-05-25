<script lang="ts" module>
  export type VolumeChapter = {
    id: string;
    title: string;
    fileName: string;
    isRead: boolean;
  };

  export type Volume = {
    id: string;
    title: string;
    chapters: VolumeChapter[];
    totalChapters: number;
    hasMore: boolean;
  };

  export type VolumePage = {
    page: number;
    items: VolumeChapter[];
  };
</script>

<script lang="ts">
  import { slide } from "svelte/transition";
  import AcerolaHeroButton from "$lib/components/acerola-hero-button/acerola-hero-button.svelte";
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";
  import Folder from "@lucide/svelte/icons/folder";
  import ChevronDown from "@lucide/svelte/icons/chevron-down";
  import BookOpen from "@lucide/svelte/icons/book-open";
  import MoreVertical from "@lucide/svelte/icons/more-vertical";
  import RefreshCw from "@lucide/svelte/icons/refresh-cw";
  import { onMount } from "svelte";

  let {
    volumes = [],
    pagesData = [],
    loading = false,
    pageSize = 25,
    onexpand,
    onvisiblepages,
  }: {
    volumes: Volume[];
    pagesData?: VolumePage[];
    loading?: boolean;
    pageSize?: number;
    onexpand: (volumeId: string | null) => void;
    onvisiblepages?: (pages: number[]) => void;
  } = $props();

  let expandedVolumeId = $state<string | null>(null);
  let visiblePages = new Set<number>();
  let observer: IntersectionObserver | null = null;

  const toggleVolume = (volumeId: string) => {
    // FIXME: Imperativo porco cheio de coisa que dá pra melhorar
    if (expandedVolumeId === volumeId) {
      expandedVolumeId = null;
      visiblePages.clear();
      if (onvisiblepages) onvisiblepages([]);
    } else {
      expandedVolumeId = volumeId;
    }
    onexpand(expandedVolumeId);
  };

  const ITEM_HEIGHT = 92;

  onMount(() => {
    observer = new IntersectionObserver(
      (entries) => {
        let changed = false;

        // FIXME: Imperativo porco cheio de coisa que dá pra melhorar
        for (const entry of entries) {
          const pageStr = (entry.target as HTMLElement).dataset.page;
          if (!pageStr) continue;
          const page = parseInt(pageStr, 10);
          if (entry.isIntersecting) {
            if (!visiblePages.has(page)) {
              visiblePages.add(page);
              changed = true;
            }
          } else {
            if (visiblePages.has(page)) {
              visiblePages.delete(page);
              changed = true;
            }
          }
        }
        if (changed && onvisiblepages) {
          onvisiblepages(Array.from(visiblePages).sort((a, b) => a - b));
        }
      },
      { rootMargin: "1000px 0px" },
    );
    return () => observer?.disconnect();
  });

  function trackPage(node: HTMLElement, page: number) {
    node.dataset.page = page.toString();
    observer?.observe(node);
    return {
      destroy() {
        observer?.unobserve(node);
      },
    };
  }
</script>

<div class="space-y-4">
  {#if volumes && volumes.length > 0}
    {#each volumes as volume}
      <AcerolaHeroButton
        title={volume.title}
        description="{volume.totalChapters} Capítulos inclusos"
        onclick={() => toggleVolume(volume.id)}
        class="bg-mantle/40 border-surface/40"
      >
        {#snippet icon()}
          <Folder size={28} />
        {/snippet}
        {#snippet action()}
          <div
            class="transition-transform duration-300 {expandedVolumeId ===
            volume.id
              ? 'rotate-180'
              : ''}"
          >
            <ChevronDown size={20} />
          </div>
        {/snippet}
      </AcerolaHeroButton>

      {#if expandedVolumeId === volume.id}
        {@const totalPages = Math.ceil(volume.totalChapters / pageSize)}

        <div transition:slide class="ml-10 border-l border-surface/30 p-4 pt-0">
          <div class="space-y-0 w-full flex flex-col">
            {#if volume.totalChapters > 0}
              {#each Array(totalPages) as _, pageIndex}
                {@const blockData = pagesData.find((p) => p.page === pageIndex)}
                {@const itemsInThisPage =
                  pageIndex === totalPages - 1
                    ? volume.totalChapters % pageSize || pageSize
                    : pageSize}

                <div
                  use:trackPage={pageIndex}
                  class="w-full flex flex-col"
                  style:min-height="{itemsInThisPage * ITEM_HEIGHT}px"
                >
                  {#if blockData && blockData.items.length > 0}
                    <div class="flex flex-col space-y-3 pt-3">
                      {#each blockData.items as chapter (chapter.id)}
                        <div style:height="80px" class="w-full">
                          <AcerolaHeroButton
                            title={chapter.title}
                            description={chapter.fileName}
                            class="bg-mantle/40 border-surface/40 hover:bg-surface/30 h-full"
                          >
                            {#snippet icon()}
                              <div
                                class={chapter.isRead
                                  ? "text-overlay"
                                  : "text-primary"}
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
                    </div>
                  {/if}
                </div>
              {/each}
            {/if}

            {#if loading}
              <div class="flex items-center justify-center py-4">
                <RefreshCw size={24} class="animate-spin text-primary" />
              </div>
            {/if}

            {#if volume.totalChapters === 0 && !loading}
              <p
                class="py-4 text-center text-xs opacity-50 uppercase font-black tracking-widest"
              >
                Nenhum capítulo disponível.
              </p>
            {/if}
          </div>
        </div>
      {/if}
    {/each}
  {:else}
    <div
      class="py-20 text-center opacity-50 space-y-4 border-2 border-dashed border-surface rounded-3xl"
    >
      <RefreshCw size={48} class="animate-spin text-primary" />
      <p class="font-black uppercase tracking-widest text-sm">
        Nenhum volume indexado ainda.
      </p>
    </div>
  {/if}
</div>
