<script lang="ts">
  import AcerolaSidebar from "$lib/components/acerola-sidebar/acerola-sidebar.svelte";
  import SidebarProvider from "$lib/components/ui/sidebar/sidebar-provider.svelte";
  import "$theme/layout.css";

  const { children } = $props();
</script>

<div class="flex h-screen">
  <SidebarProvider>
    <AcerolaSidebar />

    <main>
      {@render children()}
    </main>
  </SidebarProvider>
</div>
