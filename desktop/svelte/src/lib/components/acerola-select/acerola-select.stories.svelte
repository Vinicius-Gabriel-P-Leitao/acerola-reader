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
      placeholder: { description: "Texto exibido quando nada esta selecionado", control: "text" },
      options: { description: "Array de opcoes ({ value, label })", control: "object" },
    },
  });

  const defaultOptions: AcerolaSelectOption[] = [
    { value: "manga", label: "Manga" },
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
  name="Placeholder customizado"
  args={{
    options: defaultOptions,
    placeholder: "Escolha um formato",
  }}
/>
