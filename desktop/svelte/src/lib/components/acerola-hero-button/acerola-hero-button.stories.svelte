<script lang="ts" module>
  import { defineMeta } from "@storybook/addon-svelte-csf";
  import AcerolaHeroButton from "./acerola-hero-button.svelte";
  import FolderIcon from "@lucide/svelte/icons/folder";
  import FileTextIcon from "@lucide/svelte/icons/file-text";
  import PlayIcon from "@lucide/svelte/icons/play";

  const { Story } = defineMeta({
    title: "Components/AcerolaHeroButton",
    component: AcerolaHeroButton,
    tags: ["autodocs"],
    argTypes: {
      title: { control: "text" },
      description: { control: "text" },
    },
  });
</script>

<Story name="Com Botão de Ação">
  <AcerolaHeroButton
    title="Pasta dos mangás"
    description="Pasta onde será buscado e salvo."
    onclick={() => alert("Clicado!")}
  >
    {#snippet icon()}
      <FolderIcon size={24} />
    {/snippet}

    {#snippet action()}
      <div
        class="w-10 h-10 bg-muted rounded-full flex items-center justify-center group-hover:bg-primary group-hover:text-primary-foreground transition-all"
      >
        <PlayIcon size={20} class="ml-1" />
      </div>
    {/snippet}
  </AcerolaHeroButton>
</Story>

<Story name="Com Toggle / Switch">
  <AcerolaHeroButton
    title="Gerar ComicInfo.xml"
    description="Criar arquivo de metadados na pasta ao sincronizar."
  >
    {#snippet icon()}
      <FileTextIcon size={24} class="text-secondary" />
    {/snippet}

    {#snippet action()}
      <label class="relative inline-flex items-center cursor-pointer">
        <input type="checkbox" class="sr-only peer" checked />
        <div
          class="w-14 h-8 bg-muted peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-1 after:left-1 after:bg-foreground after:border-border after:border after:rounded-full after:h-6 after:w-6 after:transition-all peer-checked:bg-primary"
        ></div>
      </label>
    {/snippet}
  </AcerolaHeroButton>
</Story>

<Story name="Apenas Título">
  <AcerolaHeroButton title="Apenas um título básico" />
</Story>
