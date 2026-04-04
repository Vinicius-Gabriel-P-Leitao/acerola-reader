<script lang="ts" module>
  import { defineMeta } from "@storybook/addon-svelte-csf";
  import AcerolaSelect from "./acerola-select.svelte";
  import type { AcerolaSelectOption } from "./acerola-select.types";

  const { Story } = defineMeta({
    title: "Components/AcerolaSelect",
    component: AcerolaSelect,
    tags: ["autodocs"],
    argTypes: {
      value: { description: "Valor selecionado atualmente", control: "text" },
      placeholder: { description: "Texto exibido quando nada está selecionado", control: "text" },
      options: { description: "Array de opções ({ value, label })", control: "object" },
      disabled: { description: "Desabilita o componente", control: "boolean" }
    },
  });

  const defaultOptions: AcerolaSelectOption[] = [
    { value: "manga", label: "Mangá" },
    { value: "comic", label: "Comic Book" },
    { value: "webtoon", label: "Webtoon" },
  ];
</script>

<Story
  name="Default"
  args={{
    options: defaultOptions,
    placeholder: "Selecione o tipo...",
  }}
/>

<Story
  name="Com valor predefinido"
  args={{
    options: defaultOptions,
    value: "manga",
  }}
/>

<Story
  name="Desabilitado"
  args={{
    options: defaultOptions,
    disabled: true,
    placeholder: "Não disponível",
  }}
/>
