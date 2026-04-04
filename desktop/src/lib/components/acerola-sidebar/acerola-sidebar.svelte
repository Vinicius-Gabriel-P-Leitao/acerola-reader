<script lang="ts">
  import BookOpenIcon from "@lucide/svelte/icons/book-open";
  import { page } from "$app/state";
  import * as Sidebar from "$lib/components/ui/sidebar/index.js";
  import type { SidebarItem } from "./sidebar.types";

  let { items }: { items: SidebarItem[] } = $props();
</script>

<Sidebar.Root collapsible="icon">
  <Sidebar.Header class="p-6">
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
  </Sidebar.Header>

  <Sidebar.Content>
    <Sidebar.Group class="mt-4 px-3">
      <Sidebar.GroupContent>
        <Sidebar.Menu class="gap-2">
          {#each items as item (item.href)}
            <Sidebar.MenuItem>
              <Sidebar.MenuButton
                isActive={page.url.pathname === item.href}
                tooltipContent={item.label}
                class="h-12 rounded-xl px-3 [&_svg]:size-6
									data-[active=true]:bg-primary
									data-[active=true]:text-primary-foreground
									data-[active=true]:shadow-lg
									hover:bg-surface"
              >
                {#snippet child({ props })}
                  <a href={item.href} {...props}>
                    <item.icon />
                    <span>{item.label}</span>
                  </a>
                {/snippet}
              </Sidebar.MenuButton>
            </Sidebar.MenuItem>
          {/each}
        </Sidebar.Menu>
      </Sidebar.GroupContent>
    </Sidebar.Group>
  </Sidebar.Content>
</Sidebar.Root>
