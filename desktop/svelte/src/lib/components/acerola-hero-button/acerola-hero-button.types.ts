import type { Snippet } from "svelte";

export type AcerolaHeroButtonProps = {
  title?: string;
  class?: string;
  description?: string;
  icon?: Snippet;
  action?: Snippet;
  children?: Snippet;
  onclick?: (event: MouseEvent) => void;
};
