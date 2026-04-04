<script lang="ts" module>
  import { defineMeta } from "@storybook/addon-svelte-csf";
  import AcerolaButton from "./acerola-button.svelte";

  const { Story } = defineMeta({
    title: "Components/AcerolaButton",
    component: AcerolaButton,
    tags: ["autodocs"],
    argTypes: {
      variant: { description: "Variação visual do botão", control: "select", options: ["default", "outline", "ghost", "destructive"] },
      disabled: { description: "Desabilita o botão", control: "boolean" }
    }
  });
</script>

<Story name="Default" asChild>
  <AcerolaButton>Clique aqui</AcerolaButton>
</Story>

<Story name="Outline" asChild>
  <AcerolaButton variant="outline">Outline</AcerolaButton>
</Story>

<Story name="Ghost" asChild>
  <AcerolaButton variant="ghost">Ghost</AcerolaButton>
</Story>

<Story name="Destructive" asChild>
  <AcerolaButton variant="destructive">Deletar</AcerolaButton>
</Story>

<Story name="Disabled" asChild>
  <AcerolaButton disabled>Desabilitado</AcerolaButton>
</Story>
