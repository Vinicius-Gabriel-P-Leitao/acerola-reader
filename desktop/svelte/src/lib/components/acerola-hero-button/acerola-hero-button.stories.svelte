<script lang="ts" module>
  import { defineMeta } from "@storybook/addon-svelte-csf";
  import AcerolaHeroButton from "./acerola-hero-button.svelte";
  import FolderIcon from "@lucide/svelte/icons/folder";
  import PlayIcon from "@lucide/svelte/icons/play";

  const { Story } = defineMeta({
    title: "Components/AcerolaHeroButton",
    component: AcerolaHeroButton,
    tags: ["autodocs"],
    args: {
      title: "Título Padrão",
      description: "Descrição padrão do componente",
    },
  });
</script>

{#snippet folderIcon()}
  <FolderIcon size={24} />
{/snippet}

{#snippet actionButton()}
  <div
    class="p-2 rounded-full bg-primary/10 text-primary group-hover:bg-primary group-hover:text-primary-foreground transition-all"
  >
    <PlayIcon size={20} />
  </div>
{/snippet}

<Story
  name="Completo"
  args={{
    title: "Pasta dos Mangás",
    description: "C:/Users/acerola/mangas",
    icon: folderIcon,
    action: actionButton,
    onclick: () => console.log("Clicou!"),
  }}
/>

<Story
  name="Apenas Texto"
  args={{
    title: "Título de Exemplo",
    description: "Esta descrição deve aparecer agora.",
  }}
/>

<Story name="Uso Manual">
  <AcerolaHeroButton
    title="Uso Manual"
    description="Passando props diretamente no componente"
    icon={folderIcon}
    action={actionButton}
  />
</Story>
