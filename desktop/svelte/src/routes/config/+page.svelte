<script lang="ts">
  import { useTheme } from "$lib/hooks/use-theme.svelte";
  import ThemePicker from "./components/theme-picker.svelte";
  import { m } from "$lib/paraglide/messages";
  import AcerolaHeroButton from "$lib/components/acerola-hero-button/acerola-hero-button.svelte";
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";

  import FileTextIcon from "@lucide/svelte/icons/file-text";
  import FolderIcon from "@lucide/svelte/icons/folder";
  import PlayIcon from "@lucide/svelte/icons/play";
  import { useLibrary } from "$lib/hooks/use-library.svelte";
  import AcerolaSwitch from "$lib/components/acerola-switch/acerola-switch.svelte";
  import { useComicInfoPreference } from "$lib/hooks/use-comic-info-preference.svelte";
  import AcerolaInput from "$lib/components/acerola-input/acerola-input.svelte";

  const ctx = useTheme();
  const library = useLibrary();
  const comicInfoPreference = useComicInfoPreference();

  $effect(() => {
    library.loadSavedPath();
  });

  $effect(() => {
    comicInfoPreference.loadSavedComicInfoPreference();
  });
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
        description={m["pages.config.file_system.manga_path.desc"]({
          path: library.folderPath ?? "",
        })}
        onclick={library.selectFolder}
      >
        {#snippet icon()}
          <FolderIcon class="text-chart-5" size={24} />
        {/snippet}

        {#snippet action()}
          <AcerolaButtonIcon
            class="rounded-full group-hover:bg-primary group-hover:text-primary-foreground transition-all"
          >
            <PlayIcon />
          </AcerolaButtonIcon>
        {/snippet}
      </AcerolaHeroButton>

      <!-- Item: Gerar ComicInfo.xml para os mangás -->
      <AcerolaHeroButton
        title={m["pages.config.file_system.comic_info.title"]()}
        description={m["pages.config.file_system.comic_info.desc"]()}
      >
        {#snippet icon()}
          <FileTextIcon class="text-chart-2" size={24} />
        {/snippet}

        {#snippet action()}
          <AcerolaSwitch
            checked={comicInfoPreference.comicInfoPreference ?? false}
            onCheckedChange={async () => {
              await comicInfoPreference.selectComicInfoPreference();
            }}
          />
        {/snippet}
      </AcerolaHeroButton>
    </div>
  </section>

  <!-- Aparência (Componente Existente) -->
  <ThemePicker theme={ctx.theme} mode={ctx.mode} onselect={ctx.setTheme} />

  <!-- Metadados -->

  <section class="space-y-4">
    <div
      class="flex items-center gap-3 text-muted-foreground uppercase text-xs font-bold tracking-widest"
    >
      <FileTextIcon size={16} />
      <!-- FIXME: Traduzir para *.config.metadata.title com Configuração de metadados -->
      {m["pages.config.file_system.title"]()}
    </div>

    <div class="grid gap-4">
      <AcerolaHeroButton
        /* FIXME: Traduzir isso também, *config.metadata.lang.title e .desc */
        title={m["pages.config.file_system.manga_path.title"]()}
        description={m["pages.config.file_system.manga_path.desc"]({
          path: library.folderPath ?? "",
        })}
        onclick={() => console.log("teste")}
      >
        {#snippet icon()}
          <FolderIcon class="text-chart-5" size={24} />
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
</div>
