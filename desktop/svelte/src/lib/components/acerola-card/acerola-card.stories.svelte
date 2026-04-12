<script lang="ts" module>
  import { defineMeta } from "@storybook/addon-svelte-csf";
  import AcerolaCard from "./acerola-card.svelte";

  const { Story } = defineMeta({
    title: "Components/AcerolaCard",
    component: AcerolaCard,
    tags: ["autodocs"],
    parameters: {
      docs: {
        description: {
          component: "Wrapper do Card do shadcn/ui com interface simplificada via snippets.",
        },
      },
    },
    argTypes: {
      title: { description: "Título do card", control: "text" },
      description: { description: "Descrição opcional abaixo do título", control: "text" },
      size: { description: "Tamanho do card", control: "radio", options: ["default", "sm"] },
    },
  });
</script>

<Story name="Default" args={{ title: "Berserk" }} />

<Story name="Com descrição" args={{ title: "Berserk", description: "Kentaro Miura" }} />

<Story name="Tamanho sm" args={{ title: "Vagabond", description: "Takehiko Inoue", size: "sm" }} />

<Story name="Com conteúdo e footer" asChild>
  <div class="w-sm">
    <AcerolaCard
      title="Configurações da biblioteca"
      description="Ajuste como os quadrinhos são organizados e escaneados."
    >
      {#snippet children()}
        <div class="flex flex-col gap-4">
          <AcerolaInput />
          <AcerolaInput type="password" placeholder="Senha" />
        </div>
      {/snippet}

      {#snippet footer()}
        <div class="flex gap-2 w-full">
          <AcerolaButton variant="outline" class="flex-1">Cancelar</AcerolaButton>
          <AcerolaButton class="flex-1">Salvar</AcerolaButton>
        </div>
      {/snippet}
    </AcerolaCard>
  </div>
</Story>

<script>
  import AcerolaButton from "$lib/components/acerola-button/acerola-button.svelte";
  import AcerolaInput from "$lib/components/acerola-input/acerola-input.svelte";
</script>
