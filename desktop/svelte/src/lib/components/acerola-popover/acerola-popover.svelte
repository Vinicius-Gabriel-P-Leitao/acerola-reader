<script lang="ts">
  import * as Popover from "$lib/components/ui/popover/index.js";
  import type { Snippet } from "svelte";

  let {
    trigger,
    content,
    open = $bindable(false),
  }: {
    trigger: Snippet;
    content: Snippet;
    open?: boolean;
  } = $props();
</script>

<Popover.Root bind:open>
  <Popover.Trigger>
    {@render trigger()}
  </Popover.Trigger>
  <Popover.Content>
    {@render content()}
  </Popover.Content>
</Popover.Root>
