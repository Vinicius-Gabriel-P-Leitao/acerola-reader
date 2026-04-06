import type { ToggleGroup as ToggleGroupPrimitive } from "bits-ui";
import type { Snippet } from "svelte";

export type AcerolaToggleGroupProps = ToggleGroupPrimitive.RootProps & {
  children: Snippet;
  variant?: "default" | "outline";
  size?: "default" | "sm" | "lg";
};
