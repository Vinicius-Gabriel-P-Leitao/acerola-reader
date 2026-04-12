import type { Snippet } from "svelte";

export type AcerolaCardImageProps = {
  title: string;
  cover?: string | null;
  progress?: number;
  description?: string;
  placeholder?: Snippet;
  footer?: Snippet;
  action?: Snippet;
  overlay?: Snippet;
  class?: string;
};
