<script lang="ts" module>
  import { type ThemeColor, type ThemeMode } from "$lib/hooks/use-theme.svelte";
  import { m } from "$lib/paraglide/messages";

  const themes: {
    name: () => string;
    id: ThemeColor;
    description: () => string;
    colors: Record<ThemeMode, string[]>;
  }[] = [
    {
      id: "catppuccin",
      name: m["theme.catppuccin.name"],
      description: m["theme.catppuccin.desc"],
      colors: {
        light: ["#8839EF", "#EA76CB", "#1E66F5", "#EFF1F5"],
        dark: ["#CBA6F7", "#F5C2E7", "#89B4FA", "#1E1E2E"],
      },
    },
    {
      id: "nord",
      name: m["theme.nord.name"],
      description: m["theme.nord.desc"],
      colors: {
        light: ["#5E81AC", "#81A1C1", "#88C0D0", "#ECEFF4"],
        dark: ["#88C0D0", "#81A1C1", "#5E81AC", "#2E3440"],
      },
    },
    {
      id: "dracula",
      name: m["theme.dracula.name"],
      description: m["theme.dracula.desc"],
      colors: {
        light: ["#2D005F", "#6272A4", "#005A5F", "#F8F8F2"],
        dark: ["#BD93F9", "#FF79C6", "#8BE9FD", "#282A36"],
      },
    },
  ];
</script>

<script lang="ts">
  import PaletteIcon from "@lucide/svelte/icons/palette";
  import RefreshCwIcon from "@lucide/svelte/icons/refresh-cw";

  let {
    theme,
    mode,
    onselect,
  }: {
    mode: ThemeMode;
    theme: ThemeColor;
    onselect: (name: ThemeColor) => void;
  } = $props();
</script>

<section class="space-y-4">
  <div
    class="flex items-center gap-3 text-muted-foreground uppercase text-xs font-bold tracking-widest"
  >
    <PaletteIcon size={16} />
    {m["pages.config.components.theme_piker"]()}
  </div>

  <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
    {#each themes as it}
      <button
        onclick={() => onselect(it.id)}
        class="p-6 rounded-3xl border-2 transition-all text-left relative overflow-hidden cursor-pointer
          {theme === it.id
          ? 'border-primary bg-primary/5 shadow-xl shadow-primary/10'
          : 'border-border bg-card hover:border-muted-foreground'}"
      >
        <div class="flex items-center gap-4 mb-4">
          <div class="flex -space-x-2">
            {#each it.colors[mode] as color}
              <div
                class="w-8 h-8 rounded-full border-2 border-card shadow-sm"
                style="background-color: {color}"
              ></div>
            {/each}
          </div>
        </div>

        <h3 class="font-bold text-foreground">{it.name()}</h3>
        <p class="text-xs text-muted-foreground mt-1">{it.description()}</p>

        {#if theme === it.id}
          <div
            class="absolute top-4 right-4 w-6 h-6 bg-primary rounded-full flex items-center justify-center"
          >
            <RefreshCwIcon
              size={12}
              class="animate-spin-slow text-primary-foreground"
            />
          </div>
        {/if}
      </button>
    {/each}
  </div>
</section>
