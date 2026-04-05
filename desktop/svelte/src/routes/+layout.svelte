<script lang="ts" module>
  import type { SidebarItem } from "$lib/components/acerola-sidebar/sidebar.types";
  import type { AcerolaSelectOption } from "$lib/components/acerola-select/acerola-select.types";
  import type { Locale } from "$lib/paraglide/runtime.js";

  import { locales } from "$lib/paraglide/runtime.js";
  import { m } from "$lib/paraglide/messages";

  import HouseIcon from "@lucide/svelte/icons/house";
  import HistoryIcon from "@lucide/svelte/icons/history";
  import SettingsIcon from "@lucide/svelte/icons/settings";

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
  import AcerolaSidebar from "$lib/components/acerola-sidebar/acerola-sidebar.svelte";
  import AcerolaSelect from "$lib/components/acerola-select/acerola-select.svelte";
  import AcerolaModePicker from "$lib/components/acerola-mode-picker/acerola-mode-picker.svelte";
  import AcerolaSonner from "$lib/components/acerola-sonner/acerola-sonner.svelte";
  import SidebarProvider from "$lib/components/ui/sidebar/sidebar-provider.svelte";
  import { getLocale, setLocale } from "$lib/paraglide/runtime.js";

  import "$theme/layout.css";

  let currentLocale = $state(getLocale());

  $effect(() => {
    setLocale(currentLocale as Locale);
  });

  const { children } = $props();
</script>

<div class="flex h-screen">
  <SidebarProvider>
    <AcerolaSonner />
    
    <AcerolaSidebar items={sidebarItems}>
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

    <main class="flex-1">
      {@render children()}
    </main>
  </SidebarProvider>
</div>
