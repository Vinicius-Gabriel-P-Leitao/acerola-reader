import type { Snippet } from "svelte";

export type AcerolaCommandProps = {
  children: Snippet;
  value?: string;
  onValueChange?: (value: string) => void;
};
