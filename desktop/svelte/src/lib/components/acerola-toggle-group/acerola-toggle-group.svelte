<script lang="ts">
  import * as ToggleGroup from "$lib/components/ui/toggle-group/index.js";
  import type { AcerolaToggleGroupProps } from "./acerola-toggle-group.types.js";

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
