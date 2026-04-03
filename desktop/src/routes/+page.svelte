<script lang="ts">
  import AcerolaButton from "$lib/components/acerola-button/acerola-button.svelte";
  import { useLibrary } from "$lib/hooks/use-library.svelte";
  import { setLocale } from "$lib/paraglide/runtime";
  import { m } from "$lib/paraglide/messages";

  const library = useLibrary();

  $effect(() => {
    library.loadSavedPath();
  });
</script>

<AcerolaButton onclick={() => setLocale("en")}>en</AcerolaButton>
<AcerolaButton onclick={() => setLocale("pt-br")}>pt-br</AcerolaButton>

<AcerolaButton onclick={library.selectFolder}>
  Selecionar biblioteca
</AcerolaButton>

{#if library.folderPath}
  <p class="text-white">
    {m.select_path({ path: library.folderPath })}
  </p>
{/if}
