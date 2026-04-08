<script lang="ts" module>
  import MonitorIcon from "@lucide/svelte/icons/monitor";
  import MoonIcon from "@lucide/svelte/icons/moon";
  import SunIcon from "@lucide/svelte/icons/sun";

  const MODE_CONFIG: Record<ThemeModeOption, ModePickerProps> = {
    light: { icon: SunIcon, next: "dark" },
    dark: { icon: MoonIcon, next: "system" },
    system: { icon: MonitorIcon, next: "light" },
  } as const;
</script>

<script lang="ts">
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";
  import { useTheme, type ThemeModeOption } from "$lib/hooks/theme/use-theme.svelte";
  import type { ModePickerProps } from "./acerola-mode-picker.types";

  const themeCtx = useTheme();

  function nextMode() {
    themeCtx.setMode(MODE_CONFIG[themeCtx.mode].next as any);
  }
</script>

<AcerolaButtonIcon onclick={nextMode} title="Mudar tema">
  {#key themeCtx.mode}
    {@const Icon = MODE_CONFIG[themeCtx.mode].icon}
    <Icon size={16} />
  {/key}
</AcerolaButtonIcon>
