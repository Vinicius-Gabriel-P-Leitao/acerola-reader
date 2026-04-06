<script lang="ts" module>
  import { defineMeta } from "@storybook/addon-svelte-csf";
  import AcerolaToggleGroup from "./acerola-toggle-group.svelte";
  import type { AcerolaToggleGroupProps } from "./acerola-toggle-group.types.js";
  import { ToggleGroupItem } from "$lib/components/ui/toggle-group/index.js";

  const { Story } = defineMeta({
    title: "Components/AcerolaToggleGroup",
    component: AcerolaToggleGroup,
    tags: ["autodocs"],
    parameters: {
      controls: {
        include: ["type", "variant", "size", "value"],
      },
    },
    argTypes: {
      type: {
        description: "Modo de selecao do grupo",
        control: "select",
        options: ["single", "multiple"],
      },
      variant: {
        description: "Variacao visual do grupo",
        control: "select",
        options: ["default", "outline"],
      },
      size: {
        description: "Tamanho dos itens",
        control: "select",
        options: ["sm", "default", "lg"],
      },
      value: {
        description: "Valor selecionado",
        control: false,
      },
    },
  });
</script>

{#snippet template(args: AcerolaToggleGroupProps)}
  <AcerolaToggleGroup {...args}>
    {#snippet children()}
      <ToggleGroupItem value="a">Opcao A</ToggleGroupItem>
      <ToggleGroupItem value="b">Opcao B</ToggleGroupItem>
      <ToggleGroupItem value="c">Opcao C</ToggleGroupItem>
      {/snippet}
  </AcerolaToggleGroup>
{/snippet}

<Story
  name="Single Default"
  args={{ type: "single", variant: "default", size: "default", value: "a" }}
  {template}
/>

<Story
  name="Single Outline"
  args={{ type: "single", variant: "outline", size: "default", value: "b" }}
  {template}
/>

<Story
  name="Multiple Default"
  args={{ type: "multiple", variant: "default", size: "default", value: ["a", "c"] }}
  {template}
/>

<Story
  name="Multiple Outline"
  args={{ type: "multiple", variant: "outline", size: "default", value: ["b"] }}
  {template}
/>

<Story
  name="Size Small"
  args={{ type: "single", variant: "default", size: "sm", value: "a" }}
  {template}
/>

<Story
  name="Size Large"
  args={{ type: "single", variant: "default", size: "lg", value: "c" }}
  {template}
/>
