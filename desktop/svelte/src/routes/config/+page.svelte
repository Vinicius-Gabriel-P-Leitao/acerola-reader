<script lang="ts">
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";
  import AcerolaCommand from "$lib/components/acerola-command/acerola-command.svelte";
  import AcerolaHeroButton from "$lib/components/acerola-hero-button/acerola-hero-button.svelte";
  import AcerolaPopover from "$lib/components/acerola-popover/acerola-popover.svelte";
  import AcerolaSwitch from "$lib/components/acerola-switch/acerola-switch.svelte";
  import ThemePicker from "./components/theme-picker.svelte";

  import * as Command from "$lib/components/ui/command";
  import { LANGUAGES } from "$lib/constants/languages";
  import { m } from "$lib/paraglide/messages";

  import { useComicInfoPreference } from "$lib/hooks/store/use-comic-info-preference.svelte";
  import { useComicScanner } from "$lib/hooks/store/use-comic-scanner.svelte";
  import { useSelectFolder } from "$lib/hooks/store/use-select-folder.svelte";
  import { useTheme } from "$lib/hooks/theme/use-theme.svelte";
  import { onMount } from "svelte";

  import AniListIcon from "$lib/assets/icons/anilist.svg?component";
  import MangaDexIcon from "$lib/assets/icons/mangadex.svg?component";
  import CloudSync from "@lucide/svelte/icons/cloud-sync";
  import FileTextIcon from "@lucide/svelte/icons/file-text";
  import FolderIcon from "@lucide/svelte/icons/folder";
  import FolderSync from "@lucide/svelte/icons/folder-sync";
  import LanguagesIcon from "@lucide/svelte/icons/languages";
  import PlayIcon from "@lucide/svelte/icons/play";
  import Plus from "@lucide/svelte/icons/plus";
  import RefreshCw from "@lucide/svelte/icons/refresh-cw";

  const ctx = useTheme();
  const folder = useSelectFolder();
  const comicScanner = useComicScanner();
  const comicInfoPreference = useComicInfoPreference();

  onMount(async () => {
    await folder.loadSavedPath();
     
    if (folder.folderPath) {
      comicScanner.init(folder.folderPath);
    }
  });

  $effect(() => {
    folder.loadSavedPath();
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
        title={m["pages.config.file_system.comic_path.title"]()}
        description={m["pages.config.file_system.comic_path.desc"]({
          path: folder.folderPath ?? "",
        })}
        onclick={folder.selectFolder}
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

      <!-- Item: Iniciar sincronização rápida -->
      <AcerolaHeroButton
        title={m["pages.config.file_system.sync.fast.title"]()}
        description={m["pages.config.file_system.sync.fast.desc"]()}
        /* FIXME: Criar hook que vai chamar invoke do tauri e buscar os dados */
        onclick={() => console.log("sync")}
      >
        {#snippet icon()}
          <FolderSync class="text-chart-3" size={24} />
        {/snippet}

        {#snippet action()}
          <AcerolaButtonIcon
            class="rounded-full group-hover:bg-primary group-hover:text-primary-foreground transition-all"
          >
            <RefreshCw />
          </AcerolaButtonIcon>
        {/snippet}
      </AcerolaHeroButton>

      <!-- Item: Sincronização profunda, reescreve tudo do banco de dados -->
      <AcerolaHeroButton
        title={m["pages.config.file_system.sync.deep.title"]()}
        description={m["pages.config.file_system.sync.deep.desc"]()}
        /* FIXME: Criar hook que vai chamar invoke do tauri e buscar os dados */
        onclick={() => console.log("sync")}
      >
        {#snippet icon()}
          <FolderSync class="text-chart-1" size={24} />
        {/snippet}

        {#snippet action()}
          <AcerolaButtonIcon
            class="rounded-full group-hover:bg-primary group-hover:text-primary-foreground transition-all"
          >
            <RefreshCw />
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
  <ThemePicker theme={ctx.theme} mode={ctx.resolved} onselect={ctx.setTheme} />

  <!-- Metadados -->

  <section class="space-y-4">
    <div
      class="flex items-center gap-3 text-muted-foreground uppercase text-xs font-bold tracking-widest"
    >
      <CloudSync size={16} />
      {m["pages.config.metadata.title"]()}
    </div>

    <div class="grid gap-4">
      <!-- Item: Seleção do idioma dos metadados -->
      <AcerolaHeroButton
        title={m["pages.config.metadata.lang.title"]()}
        description={m["pages.config.metadata.lang.desc"]()}
        onclick={() => console.log("teste")}
      >
        {#snippet icon()}
          <LanguagesIcon class="text-chart-4" size={24} />
        {/snippet}

        {#snippet action()}
          <AcerolaPopover>
            {#snippet trigger()}
              <AcerolaButtonIcon
                class="rounded-full group-hover:bg-primary group-hover:text-primary-foreground transition-all"
              >
                <Plus />
              </AcerolaButtonIcon>
            {/snippet}

            {#snippet content()}
              <div class="w-64">
                <AcerolaCommand>
                  <Command.Input
                    placeholder={m["components.command.placeholder"]()}
                  />

                  <Command.List>
                    {#each LANGUAGES as lang}
                      <!-- FIXME: Criar hook que salva isso no sistema. -->
                      <Command.Item
                        value={lang.code}
                        class="cursor-pointer"
                        onSelect={() => console.log(lang.code)}
                      >
                        {lang.label}
                      </Command.Item>
                    {/each}
                  </Command.List>
                </AcerolaCommand>
              </div>
            {/snippet}
          </AcerolaPopover>
        {/snippet}
      </AcerolaHeroButton>

      <!-- Item: Sync com o mangadex -->
      <AcerolaHeroButton
        title={m["pages.config.metadata.mangadex.title"]()}
        description={m["pages.config.metadata.mangadex.desc"]()}
        /* FIXME: Criar hook que vai chamar invoke do tauri e salvar os dados */
        onclick={() => console.log("sync")}
      >
        {#snippet icon()}
          <MangaDexIcon class="w-6 h-6 rounded-lg" />
        {/snippet}

        {#snippet action()}
          <AcerolaButtonIcon
            class="rounded-full group-hover:bg-primary group-hover:text-primary-foreground transition-all"
          >
            <RefreshCw />
          </AcerolaButtonIcon>
        {/snippet}
      </AcerolaHeroButton>

      <!-- Item: Sync com o anilist -->
      <AcerolaHeroButton
        title={m["pages.config.metadata.anilist.title"]()}
        description={m["pages.config.metadata.anilist.desc"]()}
        /* FIXME: Criar hook que vai chamar invoke do tauri e salvar os dados */
        onclick={() => console.log("sync")}
      >
        {#snippet icon()}
          <AniListIcon class="w-6 h-6 rounded-lg" />
        {/snippet}

        {#snippet action()}
          <AcerolaButtonIcon
            class="rounded-full group-hover:bg-primary group-hover:text-primary-foreground transition-all"
          >
            <RefreshCw />
          </AcerolaButtonIcon>
        {/snippet}
      </AcerolaHeroButton>
    </div>
  </section>
</div>
