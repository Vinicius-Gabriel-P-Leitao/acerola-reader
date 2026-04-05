<script module>
  import AcerolaCommand from "./acerola-command.svelte";
  import * as Command from "$lib/components/ui/command/index.js";
  export const meta = {
    title: "Components/AcerolaCommand",
    component: AcerolaCommand,
  };
</script>

<AcerolaCommand>
  <Command.Input placeholder="Pesquisar..." />
  <Command.List>
    <Command.Item value="item1">Item 1</Command.Item>
    <Command.Item value="item2">Item 2</Command.Item>
  </Command.List>
</AcerolaCommand>
