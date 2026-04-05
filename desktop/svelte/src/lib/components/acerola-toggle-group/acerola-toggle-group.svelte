<script lang="ts">
  import * as ToggleGroup from "$lib/components/ui/toggle-group/index.js";
  import type { Snippet } from "svelte";

  let {
    children,
    value = $bindable(),
    type = "single",
    variant = "default",
    size = "default",
  }: {
    children: Snippet;
    value?: any;
    type?: "single" | "multiple";
    variant?: "default" | "outline";
    size?: "default" | "sm" | "lg";
  } = $props();
</script>

<ToggleGroup.Root bind:value type={type as any} variant={variant as any} size={size as any}>
  {@render children()}
</ToggleGroup.Root>
