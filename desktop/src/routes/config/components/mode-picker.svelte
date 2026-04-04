<script lang="ts" module>
  import { type ThemeMode } from "$lib/hooks/use-theme.svelte";

  const modes: { id: ThemeMode; label: string }[] = [
    { id: "light", label: "Claro"  },
    { id: "dark",  label: "Escuro" },
  ];
</script>

<script lang="ts">
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

<section class="space-y-4">
  <div class="flex items-center gap-3 text-muted-foreground uppercase text-xs font-bold tracking-widest">
    <SunIcon size={16} />
    Modo
  </div>

  <div class="flex gap-3">
    {#each modes as it}
      <button
        onclick={() => onselect(it.id)}
        class="flex items-center gap-2 px-5 py-2.5 rounded-xl border-2 transition-all text-sm font-medium cursor-pointer
          {mode === it.id
            ? 'border-primary bg-primary/10 text-primary'
            : 'border-border bg-card text-foreground hover:border-muted-foreground'}"
      >
        {#if it.id === "light"}
          <SunIcon size={14} />
        {:else}
          <MoonIcon size={14} />
        {/if}
        {it.label}
      </button>
    {/each}
  </div>
</section>
