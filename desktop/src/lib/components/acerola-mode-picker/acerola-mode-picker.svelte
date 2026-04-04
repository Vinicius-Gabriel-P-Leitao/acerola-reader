<script lang="ts">
  import { useTheme } from "$lib/hooks/use-theme.svelte";
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";
  import SunIcon from "@lucide/svelte/icons/sun";
  import MoonIcon from "@lucide/svelte/icons/moon";

  const ctx = useTheme();
</script>

<AcerolaButtonIcon onclick={() => ctx.setMode(ctx.mode === "light" ? "dark" : "light")}>
  {#if ctx.mode === "light"}
    <SunIcon size={16} />
  {:else}
    <MoonIcon size={16} />
  {/if}
</AcerolaButtonIcon>
