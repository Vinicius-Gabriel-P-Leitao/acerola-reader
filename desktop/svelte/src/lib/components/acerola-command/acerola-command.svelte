<script lang="ts">
  import * as Command from "$lib/components/ui/command/index.js";
  import type { AcerolaCommandProps } from "./acerola-command.types.js";

  let {
    children,
    onValueChange,
    value = $bindable(""),
  }: AcerolaCommandProps = $props();
</script>

<Command.Root bind:value {onValueChange}>
  {@render children()}
</Command.Root>
