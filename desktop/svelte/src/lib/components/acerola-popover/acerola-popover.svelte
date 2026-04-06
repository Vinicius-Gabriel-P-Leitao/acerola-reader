<script lang="ts">
  import * as Popover from "$lib/components/ui/popover/index.js";
  import type { AcerolaPopoverProps } from "./acerola-popover.types.js";

  let {
    trigger,
    content,
    open = $bindable(false),
  }: AcerolaPopoverProps = $props();
</script>

<Popover.Root bind:open>
  <Popover.Trigger>
    {@render trigger()}
  </Popover.Trigger>
  <Popover.Content>
    {@render content()}
  </Popover.Content>
</Popover.Root>
