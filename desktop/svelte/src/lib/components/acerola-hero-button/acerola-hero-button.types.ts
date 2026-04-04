import type { Snippet } from "svelte";
import type { HTMLAttributes } from "svelte/elements";

export type AcerolaHeroButtonProps = HTMLAttributes<HTMLDivElement> & {
  title: string;
  description?: string;
  icon?: Snippet;
  action?: Snippet;
};
