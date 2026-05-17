<script module lang="ts">
  import type { ToggleGroup as ToggleGroupPrimitive } from "bits-ui";
  import type { Snippet } from "svelte";

  export type AcerolaToggleGroupProps = ToggleGroupPrimitive.RootProps & {
    children: Snippet;
    variant?: "default" | "outline";
    size?: "default" | "sm" | "lg";
  };
</script>

<script lang="ts">
  import * as ToggleGroup from "$lib/components/ui/toggle-group/index.js";

  let {
    children,
    value = $bindable(),
    type = "single",
    variant = "default",
    size = "default",
  }: AcerolaToggleGroupProps = $props();
</script>

{#if type === "multiple"}
  <ToggleGroup.Root
    bind:value={value as string[] | undefined}
    type="multiple"
    {variant}
    {size}
  >
    {@render children()}
  </ToggleGroup.Root>
{:else}
  <ToggleGroup.Root
    bind:value={value as string | undefined}
    type="single"
    {variant}
    {size}
  >
    {@render children()}
  </ToggleGroup.Root>
{/if}
