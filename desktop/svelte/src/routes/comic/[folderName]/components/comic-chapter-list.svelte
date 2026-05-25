<script lang="ts" module>
  export type Chapter = {
    id: string;
    title: string;
    fileName: string;
    isRead: boolean;
    chapterSort: string;
    volumeName?: string | null;
  };

  export type ChapterPage = {
    page: number;
    items: Chapter[];
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
    pagesData = [],
    totalChapters = 0,
    pageSize = 25,
    onvisiblepages,
  }: {
    pagesData: ChapterPage[];
    totalChapters: number;
    pageSize: number;
    onvisiblepages?: (pages: number[]) => void;
  } = $props();

  /**
   * ITEM_HEIGHT (112px): Altura total do slot (item + gap).
   * BUTTON_HEIGHT (100px): Altura real do botão para caber Title + Description + Padding.
   */
  const ITEM_HEIGHT = 112;
  const BUTTON_HEIGHT = 100;

  const totalPages = $derived(Math.ceil(totalChapters / pageSize));

  let visiblePages = new Set<number>();
  let observer: IntersectionObserver | null = null;

  onMount(() => {
    observer = new IntersectionObserver(
      (entries) => {
        let changed = false;

        entries.forEach((entry) => {
          const pageStr = (entry.target as HTMLElement).dataset.page;
          if (!pageStr) return;

          const page = parseInt(pageStr, 10);
          const wasVisible = visiblePages.has(page);

          if (entry.isIntersecting) {
            visiblePages.add(page);
          } else {
            visiblePages.delete(page);
          }

          if (wasVisible !== entry.isIntersecting) {
            changed = true;
          }
        });

        if (changed && onvisiblepages) {
          onvisiblepages(Array.from(visiblePages).sort((a, b) => a - b));
        }
      },
      { rootMargin: "1200px 0px" },
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

<div class="w-full flex flex-col">
  {#if totalPages > 0}
    {#each Array(totalPages) as _, pageIndex}
      {@const blockData = pagesData.find((page) => page.page === pageIndex)}
      <!-- prettier-ignore -->
      {@const itemsInThisPage = pageIndex === totalPages - 1 ? totalChapters % pageSize || pageSize : pageSize}

      <!-- 
        PAGE BLOCK (Relative): 
        Ocupa o espaço exato da página no fluxo do documento.
        Items dentro são Absolute para garantir estabilidade visual total.
      -->
      <div
        use:trackPage={pageIndex}
        class="w-full relative"
        style:height="{itemsInThisPage * ITEM_HEIGHT}px"
      >
        {#if blockData && blockData.items.length > 0}
          {#each blockData.items as chapter, i (chapter.id)}
            <div
              class="absolute left-0 right-0 animate-in fade-in duration-300"
              style:top="{i * ITEM_HEIGHT}px"
              style:height="{BUTTON_HEIGHT}px"
            >
              <AcerolaHeroButton
                title={chapter.title}
                description={chapter.fileName}
                class="bg-mantle/40 border-surface/40 hover:bg-surface/30 h-full flex-nowrap overflow-hidden"
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
        {/if}
      </div>
    {/each}
  {:else}
    <div class="py-20 text-center opacity-50 space-y-4">
      <RefreshCw size={48} class="animate-spin mx-auto text-primary" />
      <p class="font-black uppercase tracking-widest text-sm">Carregando...</p>
    </div>
  {/if}
</div>
