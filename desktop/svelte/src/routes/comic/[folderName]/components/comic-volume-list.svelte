<script lang="ts" module>
  export type VolumeChapter = {
    id: string;
    title: string;
    isRead: boolean;
  };

  export type Volume = {
    id: string;
    title: string;
    chapters: VolumeChapter[];
    totalChapters: number;
    hasMore: boolean;
  };
</script>

<script lang="ts">
  import { slide } from "svelte/transition";
  import AcerolaHeroButton from "$lib/components/acerola-hero-button/acerola-hero-button.svelte";
  import Folder from "@lucide/svelte/icons/folder";
  import ChevronDown from "@lucide/svelte/icons/chevron-down";
  import FileText from "@lucide/svelte/icons/file-text";
  import RefreshCw from "@lucide/svelte/icons/refresh-cw";
  import { onMount } from "svelte";

  let {
    volumes = [],
    loading = false,
    minPage = 0,
    pageSize = 25,
    onexpand,
    onvisiblepages,
  }: {
    volumes: Volume[];
    loading?: boolean;
    minPage?: number;
    pageSize?: number;
    onexpand: (volumeId: string | null) => void;
    onvisiblepages?: (pages: number[]) => void;
  } = $props();

  let expandedVolumeId = $state<string | null>(null);

  let visiblePages = new Set<number>();
  let observer: IntersectionObserver | null = null;

  const toggleVolume = (volumeId: string) => {
    if (expandedVolumeId === volumeId) {
      expandedVolumeId = null;
      visiblePages.clear();
      if (onvisiblepages) onvisiblepages([]);
    } else {
      expandedVolumeId = volumeId;
    }
    onexpand(expandedVolumeId);
  };

  const ITEM_HEIGHT = 72; // 64px (h-16) + 8px gap (space-y-2)

  onMount(() => {
    observer = new IntersectionObserver(
      (entries) => {
        let changed = false;
        
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
      { 
         rootMargin: "1000px 0px" // Pre-fetch com 1000px de folga na vertical
      }
    );

    return () => {
      observer?.disconnect();
    };
  });

  function trackPage(node: HTMLElement, page: number) {
    node.dataset.page = page.toString();
    observer?.observe(node);
    return {
      destroy() {
        observer?.unobserve(node);
      }
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
            class="transition-transform duration-300 {expandedVolumeId === volume.id ? 'rotate-180' : ''}"
          >
            <ChevronDown size={20} />
          </div>
        {/snippet}
      </AcerolaHeroButton>

      {#if expandedVolumeId === volume.id}
        {@const paddingTop = minPage * pageSize * ITEM_HEIGHT}
        {@const paddingBottom = Math.max(0, volume.totalChapters - (minPage * pageSize + volume.chapters.length)) * ITEM_HEIGHT}
        <div
          transition:slide
          class="ml-10 border-l border-surface/30 p-4 pt-0 relative"
        >
          <!-- Sentinelas de Página para Interseção -->
          {#if volume.totalChapters > 0}
            {@const totalPages = Math.ceil(volume.totalChapters / pageSize)}
            <div class="absolute inset-x-0 top-0 pointer-events-none" style="z-index: -1;">
              {#each Array(totalPages) as _, page}
                <div 
                  use:trackPage={page}
                  class="absolute w-full"
                  style:top="{page * pageSize * ITEM_HEIGHT}px"
                  style:height="{pageSize * ITEM_HEIGHT}px"
                ></div>
              {/each}
            </div>
          {/if}

          <div 
            class="space-y-2 w-full" 
            style:padding-top="{paddingTop}px"
            style:padding-bottom="{paddingBottom}px"
          >
            {#if volume.chapters.length > 0}
              {#each volume.chapters as chapter}
                <div style="height: 64px" class="w-full">
                  <AcerolaHeroButton
                    title={chapter.title}
                    class="bg-base/20 border-transparent hover:bg-surface/30 h-full"
                  >
                    {#snippet icon()}
                      <div
                        class="w-10 h-10 rounded-lg flex items-center justify-center {chapter.isRead ? 'bg-surface/50 text-overlay' : 'bg-primary/10 text-primary'}"
                      >
                        <FileText size={18} />
                      </div>
                    {/snippet}
                  </AcerolaHeroButton>
                </div>
              {/each}

              {#if loading}
                <div class="flex items-center justify-center py-4 relative z-10">
                  <RefreshCw size={24} class="animate-spin text-primary" />
                </div>
              {/if}
            {:else if loading}
              <div class="flex flex-col items-center justify-center py-10 space-y-4 opacity-50 relative z-10">
                  <RefreshCw size={32} class="animate-spin text-primary" />
                  <p class="font-black uppercase tracking-widest text-[10px]">Carregando capítulos...</p>
              </div>
            {:else}
              <p class="py-4 text-center text-xs opacity-50 uppercase font-black tracking-widest relative z-10">
                  Nenhum capítulo disponível.
              </p>
            {/if}
          </div>
        </div>
      {/if}
    {/each}
  {:else}
    <div class="py-20 text-center opacity-50 space-y-4 border-2 border-dashed border-surface rounded-3xl">
      <RefreshCw size={48} class="animate-spin text-primary" />
      <p class="font-black uppercase tracking-widest text-sm">
        Nenhum volume indexado ainda.
      </p>
    </div>
  {/if}
</div>
