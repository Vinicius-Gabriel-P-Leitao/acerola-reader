import type { Snippet } from "svelte";

export type AcerolaPopoverProps = {
  trigger: Snippet;
  content: Snippet;
  open?: boolean;
};
