import type { ThemeModeOption } from "$lib/hooks/use-theme.svelte";
import type { Component } from "svelte";

export type ModePickerProps = {
  icon: Component;
  next: ThemeModeOption;
};
