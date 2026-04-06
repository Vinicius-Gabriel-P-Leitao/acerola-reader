<script lang="ts" module>
  import { defineMeta } from "@storybook/addon-svelte-csf";
  import AcerolaSwitch from "./acerola-switch.svelte";

  const { Story } = defineMeta({
    title: "Components/AcerolaSwitch",
    component: AcerolaSwitch,
    tags: ["autodocs"],
    parameters: {
      controls: {
        include: ['checked', 'size', 'disabled']
      }
    },
    argTypes: {
      checked: { description: "Estado do switch", control: "boolean" },
      size: { description: "Tamanho do switch", control: "select", options: ["default", "sm"] },
      disabled: { description: "Desabilita o switch", control: "boolean" }
    }
  });
</script>

<Story name="Default" asChild>
  <AcerolaSwitch />
</Story>

<Story name="Checked" asChild>
  <AcerolaSwitch checked />
</Story>

<Story name="Small" asChild>
  <AcerolaSwitch size="sm" />
</Story>

<Story name="Disabled" asChild>
  <AcerolaSwitch disabled />
</Story>
