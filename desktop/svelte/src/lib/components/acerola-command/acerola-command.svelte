<script lang="ts">
  import * as Command from "$lib/components/ui/command/index.js";
  import type { Snippet } from "svelte";

  let {
    children,
    value = $bindable(""),
    onValueChange,
  }: {
    children: Snippet;
    value?: string;
    onValueChange?: (value: string) => void;
  } = $props();
</script>

<Command.Root bind:value onValueChange={onValueChange}>
  {@render children()}
</Command.Root>
