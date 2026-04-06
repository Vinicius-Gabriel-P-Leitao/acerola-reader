<script lang="ts" module>
  import { defineMeta } from "@storybook/addon-svelte-csf";
  import AcerolaInput from "./acerola-input.svelte";

  const { Story } = defineMeta({
    title: "Components/AcerolaInput",
    component: AcerolaInput,
    tags: ["autodocs"],
    parameters: {
      docs: {
        description: {
          component: "Campo de texto padrão da aplicação.",
        },
      },
    },
    argTypes: {
      value: { description: "Valor do campo de texto", control: "text" },
      placeholder: { description: "Placeholder do input", control: "text" },
      disabled: { description: "Desabilita o input", control: "boolean" },
      type: { description: "Tipo do input (text, search, etc)", control: "text" }
    },
  });
</script>

{#snippet template(args: Record<string, unknown>)}
  <AcerolaInput {...args} />
{/snippet}

<Story name="Default" args={{ placeholder: "Digite algo..." }} {template} />

<Story
  name="With Value"
  args={{ value: "Texto inicial", placeholder: "Digite algo..." }}
  {template}
/>

<Story
  name="Search"
  args={{ type: "search", placeholder: "Pesquisar na biblioteca..." }}
  {template}
/>

<Story
  name="Disabled"
  args={{ disabled: true, placeholder: "Campo desabilitado" }}
  {template}
/>
