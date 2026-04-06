<script lang="ts" module>
  import { defineMeta } from "@storybook/addon-svelte-csf";
  import AcerolaPopover from "./acerola-popover.svelte";

  const { Story } = defineMeta({
    title: "Components/AcerolaPopover",
    component: AcerolaPopover,
    tags: ["autodocs"],
    parameters: {
      docs: {
        description: {
          component:
            "Wrapper para exibicao de conteudo flutuante ancorado em um gatilho. O componente recebe dois snippets obrigatorios, `trigger` e `content`, e controla internamente a abertura com `bind:open`, permitindo uso controlado ou nao controlado conforme a necessidade.",
        },
      },
      controls: {
        include: ["open"],
      },
    },
    argTypes: {
      open: {
        description: "Define se o popover inicia aberto ou fechado.",
        control: "boolean",
      },
      trigger: {
        description: "Snippet que renderiza o elemento usado como gatilho do popover.",
        control: false,
      },
      content: {
        description: "Snippet com o conteudo exibido dentro da camada flutuante.",
        control: false,
      },
    },
  });
</script>

{#snippet template(args: { open?: boolean })}
  <AcerolaPopover {...args}>
    {#snippet trigger()}
      <button type="button" class="bg-primary text-primary-foreground rounded px-4 py-2">
        Abrir Popover
      </button>
    {/snippet}
    {#snippet content()}
      <div class="bg-popover text-popover-foreground rounded-lg border p-4 shadow-md">
        Conteudo do Popover
      </div>
    {/snippet}
  </AcerolaPopover>
{/snippet}

<Story
  name="Default"
  args={{ open: false }}
  parameters={{
    docs: {
      description: {
        story:
          "Estado inicial fechado. Este e o comportamento padrao para menus contextuais, dicas expandidas e acoes secundarias reveladas sob demanda.",
      },
    },
  }}
  {template}
/>

<Story
  name="Initially Open"
  args={{ open: true }}
  parameters={{
    docs: {
      description: {
        story:
          "Estado inicial aberto. E util para inspecao visual, validacao de layout e casos em que o conteudo precisa aparecer imediatamente por regra de negocio ou onboarding.",
      },
    },
  }}
  {template}
/>
