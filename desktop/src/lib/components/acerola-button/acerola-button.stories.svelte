<script lang="ts" module>
  import { defineMeta } from "@storybook/addon-svelte-csf";
  import AcerolaButton from "./acerola-button.svelte";

  const { Story } = defineMeta({
    title: "Components/AcerolaButton",
    component: AcerolaButton,
    tags: ["autodocs"],
  });
</script>

<Story name="Default">
  <AcerolaButton>Clique aqui</AcerolaButton>
</Story>

<Story name="Disabled">
  <AcerolaButton disabled>Desabilitado</AcerolaButton>
</Story>
