<script module>
  import AcerolaPopover from "./acerola-popover.svelte";
  export const meta = {
    title: "Components/AcerolaPopover",
    component: AcerolaPopover,
  };
</script>

<AcerolaPopover>
  {#snippet trigger()}
    <button class="bg-primary text-primary-foreground px-4 py-2 rounded">Abrir Popover</button>
  {/snippet}
  {#snippet content()}
    <div class="p-4 bg-popover text-popover-foreground border rounded-lg shadow-md">
      Conteúdo do Popover
    </div>
  {/snippet}
</AcerolaPopover>
