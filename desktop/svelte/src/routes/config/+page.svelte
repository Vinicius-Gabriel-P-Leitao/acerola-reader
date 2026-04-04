<script lang="ts">
  import { useTheme } from "$lib/hooks/use-theme.svelte";
  import ThemePicker from "./components/theme-picker.svelte";
  import { m } from "$lib/paraglide/messages";
  import AcerolaHeroButton from "$lib/components/acerola-hero-button/acerola-hero-button.svelte";
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";

  import FileTextIcon from "@lucide/svelte/icons/file-text";
  import FolderIcon from "@lucide/svelte/icons/folder";
  import PlayIcon from "@lucide/svelte/icons/play";

  const ctx = useTheme();
</script>

<div class="max-w-5xl p-8 space-y-12">
  <!-- Header -->
  <div>
    <h1 class="text-3xl font-bold tracking-tight text-foreground">
      {m["pages.config.title"]()}
    </h1>

    <p class="text-muted-foreground mt-2">
      {m["pages.config.desc"]()}
    </p>
  </div>

  <!-- Configuração dos Arquivos -->
  <section class="space-y-4">
    <div
      class="flex items-center gap-3 text-muted-foreground uppercase text-xs font-bold tracking-widest"
    >
      <FileTextIcon size={16} />
      {m["pages.config.file_system.title"]()}
    </div>

    <div class="grid gap-4">
      <!-- Item: Pasta dos mangás -->
      <AcerolaHeroButton
        title={m["pages.config.file_system.manga_path.title"]()}
        description={m["pages.config.file_system.manga_path.desc"]()}
        onclick={() => console.log("Abrir pasta")}
      >
        {#snippet icon()}
          <FolderIcon size={24} />
        {/snippet}

        {#snippet action()}
          <AcerolaButtonIcon
            class="rounded-full group-hover:bg-primary group-hover:text-primary-foreground transition-all"
          >
            <PlayIcon />
          </AcerolaButtonIcon>
        {/snippet}
      </AcerolaHeroButton>
    </div>
  </section>

  <!-- Aparência (Componente Existente) -->
  <ThemePicker theme={ctx.theme} mode={ctx.mode} onselect={ctx.setTheme} />
</div>
