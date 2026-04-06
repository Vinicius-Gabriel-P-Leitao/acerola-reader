<script lang="ts">
  import { page } from "$app/state";
  import * as Sidebar from "$lib/components/ui/sidebar/index.js";
  import type { AcerolaSidebarProps } from "./acerola-sidebar.types";

  let { items, header, footer, class: className }: AcerolaSidebarProps = $props();
</script>

<Sidebar.Root collapsible="icon" class={className}>
  {#if header}
    <Sidebar.Header class="p-6">
      {@render header()}
    </Sidebar.Header>
  {/if}

  <Sidebar.Content>
    <Sidebar.Group class="mt-4 px-3">
      <Sidebar.GroupContent>
        <Sidebar.Menu class="gap-2">
          {#each items as item (item.href)}
            <Sidebar.MenuItem>
              <Sidebar.MenuButton
                isActive={page.url.pathname === item.href}
                tooltipContent={item.label}
                class="h-12 rounded-xl px-3 [&_svg]:size-6 data-[active=true]:bg-primary data-[active=true]:text-primary-foreground data-[active=true]:shadow-lg hover:bg-surface"
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

  {#if footer}
    <Sidebar.Footer class="p-4">
      {@render footer()}
    </Sidebar.Footer>
  {/if}
</Sidebar.Root>
