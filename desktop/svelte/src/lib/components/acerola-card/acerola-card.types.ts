import type { Snippet } from "svelte";

export type AcerolaCardProps = {
  title: string;
  description?: string;
  children?: Snippet;
  footer?: Snippet;
  size?: "default" | "sm";
  class?: string;
};
