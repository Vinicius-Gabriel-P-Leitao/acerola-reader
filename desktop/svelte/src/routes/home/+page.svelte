<script lang="ts">
  import { useComicSummary } from "$lib/hooks/store/use-comic-summary.svelte";
  import { LIBRARY_EVENTS } from "$lib/contracts/library/library.events";
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
</script>

{#if summary.loading}
  <p>Carregando...</p>
{:else if summary.comics}
  <ul>
    {#each summary.comics.comics as comic}
      <li>
        <strong>{comic.metadata.title ?? comic.filesystem.folderName}</strong>
        <span>{comic.metadata.activeSource ?? "Sem fonte"}</span>
      </li>
    {/each}
  </ul>
{:else}
  <p>Nenhum quadrinho encontrado.</p>
{/if}
