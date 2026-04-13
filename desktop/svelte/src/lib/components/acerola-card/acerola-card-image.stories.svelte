<script lang="ts" module>
  import { defineMeta } from "@storybook/addon-svelte-csf";
  import MoreVerticalIcon from "@lucide/svelte/icons/more-vertical";
  import BookOpenIcon from "@lucide/svelte/icons/book-open";
  import AcerolaCardImage from "./acerola-card-image.svelte";

  const { Story } = defineMeta({
    title: "Components/AcerolaCardImage",
    component: AcerolaCardImage,
    tags: ["autodocs"],
    parameters: {
      docs: {
        description: {
          component: "Card de quadrinho com imagem de capa. O footer e a action são snippets livres — passe badges, contagens ou qualquer elemento.",
        },
      },
    },
    argTypes: {
      title: { description: "Título do quadrinho", control: "text" },
      cover: { description: "Caminho da imagem de capa", control: "text" },
      description: { description: "Texto exibido no overlay ao passar o mouse", control: "text" },
      progress: { description: "Progresso de leitura de 0 a 100", control: { type: "range", min: 0, max: 100 } },
    },
  });
</script>

<Story name="Sem capa" args={{ title: "Berserk" }} />

<Story name="Com progresso" args={{ title: "Berserk", progress: 65 }} />

<Story name="Com descrição no overlay" args={{ title: "Berserk", description: "Uma história sombria de espada e feitiçaria." }} />

<Story name="Com footer e action" asChild>
  <AcerolaCardImage title="Berserk">
    {#snippet footer()}
      <span class="text-[10px] uppercase tracking-wider font-black text-muted-foreground flex items-center gap-1">
        <BookOpenIcon size={10} />
        374 Caps
      </span>
    {/snippet}

    {#snippet action()}
      <button class="text-muted-foreground hover:text-primary transition-colors p-1">
        <MoreVerticalIcon size={16} />
      </button>
    {/snippet}
  </AcerolaCardImage>
</Story>

<Story name="Com overlay customizado" asChild>
  <AcerolaCardImage title="Vagabond">
    {#snippet overlay()}
      <button class="w-full bg-primary text-primary-foreground py-2 rounded-2xl text-xs font-black">
        LER
      </button>
    {/snippet}
  </AcerolaCardImage>
</Story>
