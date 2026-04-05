<script module>
  import AcerolaToggleGroup from "./acerola-toggle-group.svelte";
  import { ToggleGroupItem } from "$lib/components/ui/toggle-group/index.js";
  export const meta = {
    title: "Components/AcerolaToggleGroup",
    component: AcerolaToggleGroup,
  };
</script>

<AcerolaToggleGroup type="single">
  <ToggleGroupItem value="a">Opção A</ToggleGroupItem>
  <ToggleGroupItem value="b">Opção B</ToggleGroupItem>
</AcerolaToggleGroup>
