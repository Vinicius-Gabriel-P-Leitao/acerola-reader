import type { ThemeColor, ThemeMode } from "$lib/hooks/theme/use-theme.svelte";

export type ThemePickerProps = {
  id: ThemeColor;
  name: () => string;
  description: () => string;
  colors: Record<ThemeMode, string[]>;
};
