<script lang="ts">
  import { goto } from "$app/navigation";
  import PlaceholderManga from "$lib/assets/placeholder/placeholder_manga.svg?component";
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";
  import AcerolaCardImage from "$lib/components/acerola-card/acerola-card-image.svelte";
  import MoreHorizontal from "@lucide/svelte/icons/more-horizontal";
  import BookOpen from "@lucide/svelte/icons/book-open";

  import { LIBRARY_EVENTS } from "$lib/contracts/library/library.events";
  import { useComicSummary } from "$lib/hooks/store/use-comic-summary.svelte";
  import { useComicContext } from "$lib/state/comic-context.svelte";
  import { convertFileSrc } from "@tauri-apps/api/core";
  import { listen } from "@tauri-apps/api/event";
  import { onDestroy, onMount } from "svelte";

  const summary = useComicSummary();
  const activeComic = useComicContext();

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

  function resolveCover(artwork: {
    cover: string | null;
    banner: string | null;
  }): string | null {
    const path = artwork.cover ?? artwork.banner ?? null;
    if (!path) return null;
    return convertFileSrc(path.replaceAll("\\", "/"));
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
          data={{
            title: comic.metadata.title ?? comic.filesystem.folderName,
            cover,
          }}
          events={{
            onClick: () => {
              activeComic.set(comic, cover);
              goto(`/comic/${comic.filesystem.folderName}`);
            },
          }}
        >
          {#snippet footer()}
            <div class="flex items-center justify-between mt-1">
              <span
                class="text-[10px] uppercase tracking-wider font-black text-overlay flex items-center gap-1"
              >
                <BookOpen size={10} />
                {comic.metadata.chapterCount}
              </span>
            </div>
          {/snippet}

          {#snippet action()}
            <AcerolaButtonIcon
              class="bg-transparent text-overlay hover:text-primary transition-colors"
            >
              <MoreHorizontal size={16} />
            </AcerolaButtonIcon>
          {/snippet}

          {#snippet placeholder()}
            <div class="w-full h-full bg-surface">
              <PlaceholderManga class="w-full h-full" />
            </div>
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
