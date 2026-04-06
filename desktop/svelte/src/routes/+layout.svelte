<script lang="ts" module>
  import type { SidebarItem } from "$lib/components/acerola-sidebar/acerola-sidebar.types";
  import type { AcerolaSelectOption } from "$lib/components/acerola-select/acerola-select.types";
  import type { Locale } from "$lib/paraglide/runtime.js";

  import { locales } from "$lib/paraglide/runtime.js";
  import { m } from "$lib/paraglide/messages";

  import HouseIcon from "@lucide/svelte/icons/house";
  import HistoryIcon from "@lucide/svelte/icons/history";
  import SettingsIcon from "@lucide/svelte/icons/settings";

  // Ícones da Titlebar
  import MinusIcon from "@lucide/svelte/icons/minus";
  import SquareIcon from "@lucide/svelte/icons/square";
  import XIcon from "@lucide/svelte/icons/x";
  import BookOpenIcon from "@lucide/svelte/icons/book-open";

  const localeLabels: Record<string, string> = {
    "pt-br": "Português",
    en: "English",
  };

  const localeOptions: AcerolaSelectOption[] = locales.map((locale) => ({
    value: locale,
    label: localeLabels[locale] || locale.toUpperCase(),
  }));

  const sidebarItems: SidebarItem[] = $derived([
    { label: m["routes.home"](), href: "/home", icon: HouseIcon },
    { label: m["routes.history"](), href: "/history", icon: HistoryIcon },
    { label: m["routes.config"](), href: "/config", icon: SettingsIcon },
  ]);
</script>

<script lang="ts">
  import { onMount } from "svelte";
  import AcerolaSidebar from "$lib/components/acerola-sidebar/acerola-sidebar.svelte";
  import AcerolaSelect from "$lib/components/acerola-select/acerola-select.svelte";
  import AcerolaModePicker from "$lib/components/acerola-mode-picker/acerola-mode-picker.svelte";
  import AcerolaSonner from "$lib/components/acerola-sonner/acerola-sonner.svelte";
  import SidebarProvider from "$lib/components/ui/sidebar/sidebar-provider.svelte";
  import { getLocale, setLocale } from "$lib/paraglide/runtime.js";

  import "$theme/layout.css";

  let currentLocale = $state(getLocale());
  let appWindow = $state<any>(null);

  onMount(async () => {
    // Importação dinâmica para evitar que quebre durante o SSR (Server-Side Rendering)
    const { getCurrentWindow } = await import("@tauri-apps/api/window");
    appWindow = getCurrentWindow();
  });

  function minimize() {
    appWindow?.minimize();
  }

  function toggleMaximize() {
    appWindow?.toggleMaximize();
  }

  function closeWindow() {
    appWindow?.close();
  }

  $effect(() => {
    setLocale(currentLocale as Locale);
  });

  const { children } = $props();
</script>

<div class="flex flex-col h-screen w-full overflow-hidden">
  <!-- Titlebar Global -->
  <div
    data-tauri-drag-region
    class="h-8 shrink-0 bg-background flex justify-between items-center select-none z-100 border-b border-border/50"
  >
    <div
      data-tauri-drag-region
      class="flex-1 h-full flex items-center pl-4 text-xs font-semibold text-muted-foreground pointer-events-none"
    >
      Acerola
    </div>

    <div class="flex h-full text-foreground">
      <button
        class="h-full w-11.5 inline-flex justify-center items-center hover:bg-muted transition-colors"
        aria-label="Minimizar"
        onclick={minimize}
      >
        <MinusIcon size={16} strokeWidth={1.5} />
      </button>
      <button
        class="h-full w-11.5 inline-flex justify-center items-center hover:bg-muted transition-colors"
        aria-label="Maximizar"
        onclick={toggleMaximize}
      >
        <SquareIcon size={14} strokeWidth={1.5} />
      </button>
      <button
        class="h-full w-11.5 inline-flex justify-center items-center hover:bg-destructive hover:text-destructive-foreground transition-colors"
        aria-label="Fechar"
        onclick={closeWindow}
      >
        <XIcon size={18} strokeWidth={1.5} />
      </button>
    </div>
  </div>

  <!-- Conteúdo Principal da Aplicação -->
  <div class="flex flex-1 relative overflow-hidden transform-[translateZ(0)]">
    <SidebarProvider>
      <AcerolaSonner />

      <AcerolaSidebar items={sidebarItems} class="h-full!">
        {#snippet header()}
          <div class="flex items-center gap-3">
            <div
              class="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary text-primary-foreground"
            >
              <BookOpenIcon size={24} />
            </div>

            <span
              class="text-xl font-bold tracking-tight group-data-[collapsible=icon]:hidden"
            >
              Acerola
            </span>
          </div>
        {/snippet}

        {#snippet footer()}
          <div class="flex items-center gap-2 px-2 pb-2 w-full overflow-hidden">
            <AcerolaModePicker />

            <AcerolaSelect
              bind:value={currentLocale}
              options={localeOptions}
              class="flex-1 min-w-0"
            />
          </div>
        {/snippet}
      </AcerolaSidebar>

      <main class="flex-1 overflow-y-auto">
        {@render children()}
      </main>
    </SidebarProvider>
  </div>
</div>
