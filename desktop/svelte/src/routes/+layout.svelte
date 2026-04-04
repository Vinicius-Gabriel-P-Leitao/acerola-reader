<script lang="ts">
  import AcerolaModePicker from "$lib/components/acerola-mode-picker/acerola-mode-picker.svelte";
  import AcerolaSidebar from "$lib/components/acerola-sidebar/acerola-sidebar.svelte";
  import type { SidebarItem } from "$lib/components/acerola-sidebar/sidebar.types";
  import SidebarProvider from "$lib/components/ui/sidebar/sidebar-provider.svelte";
  import { setLocale } from "$lib/paraglide/runtime";
  import "$theme/layout.css";
  import HistoryIcon from "@lucide/svelte/icons/history";
  import HouseIcon from "@lucide/svelte/icons/house";
  import SettingsIcon from "@lucide/svelte/icons/settings";

  setLocale("pt-br");
  const { children } = $props();

  const sidebarItems: SidebarItem[] = [
    { label: "Home", href: "/home", icon: HouseIcon },
    { label: "Histórico", href: "/history", icon: HistoryIcon },
    { label: "Configurações", href: "/config", icon: SettingsIcon },
  ];
</script>

<div class="flex h-screen">
  <SidebarProvider>
    <AcerolaSidebar items={sidebarItems}>
      {#snippet footer()}
        <AcerolaModePicker />
      {/snippet}
    </AcerolaSidebar>

    <main class="flex-1">
      {@render children()}
    </main>
  </SidebarProvider>
</div>
