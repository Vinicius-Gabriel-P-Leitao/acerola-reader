<script lang="ts">
  import { useComicSummary } from "$lib/hooks/store/use-comic-summary.svelte";
  import { LIBRARY_EVENTS } from "$lib/contracts/library/library.events";
  import AcerolaCardImage from "$lib/components/acerola-card/acerola-card-image.svelte";
  import PlaceholderManga from "$lib/assets/placeholder/placeholder_manga.svg?component";
  import { convertFileSrc } from "@tauri-apps/api/core";
  import { listen } from "@tauri-apps/api/event";
  import { onMount, onDestroy } from "svelte";

  const summary = useComicSummary();

  let unlistenScan: (() => void) | undefined;

  onMount(async () => {
    await summary.fetch();

    unlistenScan = await listen(LIBRARY_EVENTS.scanComplete, async () => {
      await summary.fetch();
    });
  });

  onDestroy(() => {
    unlistenScan?.();
  });

  function resolveCover(artwork: { cover: string | null; banner: string | null }): string | null {
    const path = artwork.cover ?? artwork.banner ?? null;
    return path ? convertFileSrc(path) : null;
  }
</script>

{#if summary.loading}
  <div class="p-8 flex items-center justify-center text-muted-foreground">
    Carregando...
  </div>
{:else if summary.comics && summary.comics.total > 0}
  <div class="p-8">
    <div class="grid grid-cols-[repeat(auto-fill,minmax(9rem,1fr))] gap-6">
      {#each summary.comics.comics as comic (comic.relations.directoryId)}
        {@const cover = resolveCover(comic.artwork)}
        <AcerolaCardImage
          title={comic.metadata.title ?? comic.filesystem.folderName}
          {cover}
        >
          {#snippet placeholder()}
            <PlaceholderManga class="w-full h-full object-cover object-top" />
          {/snippet}
        </AcerolaCardImage>
      {/each}
    </div>
  </div>
{:else}
  <div class="p-8 flex items-center justify-center text-muted-foreground">
    Nenhum quadrinho encontrado.
  </div>
{/if}
