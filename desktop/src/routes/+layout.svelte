<script lang="ts">
  import AcerolaSidebar from "$lib/components/acerola-sidebar/acerola-sidebar.svelte";
  import type { SidebarItem } from "$lib/components/acerola-sidebar/sidebar.types";
  import SidebarProvider from "$lib/components/ui/sidebar/sidebar-provider.svelte";
  import "$theme/layout.css";

  import HistoryIcon from "@lucide/svelte/icons/history";
  import HouseIcon from "@lucide/svelte/icons/house";
  import SettingsIcon from "@lucide/svelte/icons/settings";

  const { children } = $props();

  const sidebarItems: SidebarItem[] = [
    { label: "Home", href: "/", icon: HouseIcon },
    { label: "Histórico", href: "/historico", icon: HistoryIcon },
    { label: "Configurações", href: "/configuracoes", icon: SettingsIcon },	
  ];
</script>

<div class="flex h-screen">
  <SidebarProvider>
    <AcerolaSidebar items={sidebarItems} />

    <main>
      {@render children()}
    </main>
  </SidebarProvider>
</div>
