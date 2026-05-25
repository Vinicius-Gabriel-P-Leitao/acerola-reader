<script lang="ts" module>
  export type Chapter = {
    id: string;
    title: string;
    fileName: string;
    isRead: boolean;
    chapterSort: string;
    volumeName?: string | null;
  };
</script>

<script lang="ts">
  import { onMount } from "svelte";
  import AcerolaHeroButton from "$lib/components/acerola-hero-button/acerola-hero-button.svelte";
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";
  import BookOpen from "@lucide/svelte/icons/book-open";
  import MoreVertical from "@lucide/svelte/icons/more-vertical";
  import RefreshCw from "@lucide/svelte/icons/refresh-cw";

  let {
    chapters = [],
    totalChapters = 0,
    minPage = 0,
    pageSize = 25,
    onvisiblepages
  }: {
    chapters: Chapter[];
    totalChapters: number;
    minPage: number;
    pageSize: number;
    onvisiblepages?: (pages: number[]) => void;
  } = $props();

  const ITEM_HEIGHT = 92;
  const totalPages = $derived(Math.ceil(totalChapters / pageSize));

  let visiblePages = new Set<number>();
  let observer: IntersectionObserver | null = null;

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

  function trackPage(node: HTMLElement) {
    observer?.observe(node);
    return {
      destroy() {
        observer?.unobserve(node);
      }
    };
  }
</script>

<!-- Container virtual posicionado de forma estrita para evitar reflow -->
<div 
  class="relative w-full overflow-hidden" 
  style:height="{totalChapters * ITEM_HEIGHT}px"
>
  {#if totalPages > 0}
    <!-- Sentinelas de página. O IntersectionObserver vigia quem entra em foco -->
    {#each Array(totalPages) as _, page}
      <div 
        use:trackPage
        data-page={page}
        class="absolute w-full pointer-events-none"
        style:top="{page * pageSize * ITEM_HEIGHT}px"
        style:height="{pageSize * ITEM_HEIGHT}px"
      ></div>
    {/each}
  {/if}

  {#if chapters && chapters.length > 0}
    {#each chapters as chapter, i (chapter.id)}
      {@const globalIndex = (minPage * pageSize) + i}
      <div 
        class="absolute left-0 right-0 animate-in fade-in duration-300"
        style:top="{globalIndex * ITEM_HEIGHT}px"
        style:height="80px"
      >
        <AcerolaHeroButton
          title={chapter.title}
          description={chapter.fileName}
          class="bg-mantle/40 border-surface/40 hover:bg-surface/30 h-full"
        >
          {#snippet icon()}
            <div class={chapter.isRead ? "text-overlay" : "text-primary"}>
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
    <div class="absolute inset-x-0 top-20 text-center opacity-50 space-y-4">
      <RefreshCw size={48} class="animate-spin mx-auto text-primary" />
      <p class="font-black uppercase tracking-widest text-sm">
        Carregando...
      </p>
    </div>
  {/if}
</div>
