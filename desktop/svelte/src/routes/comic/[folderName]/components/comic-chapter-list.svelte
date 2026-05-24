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
  import AcerolaHeroButton from "$lib/components/acerola-hero-button/acerola-hero-button.svelte";
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";
  import BookOpen from "@lucide/svelte/icons/book-open";
  import MoreVertical from "@lucide/svelte/icons/more-vertical";
  import RefreshCw from "@lucide/svelte/icons/refresh-cw";

  let {
    chapters = [],
    paddingTop = 0,
    paddingBottom = 0,
  }: {
    chapters: Chapter[];
    paddingTop?: number;
    paddingBottom?: number;
  } = $props();
</script>

<div class="relative" style:padding-top="{paddingTop}px" style:padding-bottom="{paddingBottom}px">
  <div class="space-y-3">
    {#if chapters && chapters.length > 0}
      {#each chapters as chapter (chapter.id)}
        <div class="animate-in fade-in slide-in-from-bottom-2 duration-300">
          <AcerolaHeroButton
            title={chapter.title}
            description={chapter.fileName}
            class="bg-mantle/40 border-surface/40 hover:bg-surface/30"
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
      <div class="py-20 text-center opacity-50 space-y-4">
        <RefreshCw size={48} class="animate-spin mx-auto text-primary" />
        <p class="font-black uppercase tracking-widest text-sm">
          Nenhum capítulo encontrado.
        </p>
      </div>
    {/if}
  </div>
</div>
