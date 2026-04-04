<script lang="ts">
  import { type ThemeMode } from "$lib/hooks/use-theme.svelte";
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";
  import SunIcon from "@lucide/svelte/icons/sun";
  import MoonIcon from "@lucide/svelte/icons/moon";

  let {
    mode,
    onselect,
  }: {
    mode: ThemeMode;
    onselect: (mode: ThemeMode) => void;
  } = $props();
</script>

<AcerolaButtonIcon onclick={() => onselect(mode === "light" ? "dark" : "light")}>
  {#if mode === "light"}
    <SunIcon size={16} />
  {:else}
    <MoonIcon size={16} />
  {/if}
</AcerolaButtonIcon>
