<script lang="ts" module>
  import { createNotifications } from "$lib/state/notification.svelte";
  import { tv } from "tailwind-variants";

  import AlertTriangleIcon from "@lucide/svelte/icons/alert-triangle";
  import BellIcon from "@lucide/svelte/icons/bell";
  import CheckCircle2Icon from "@lucide/svelte/icons/check-circle-2";
  import InfoIcon from "@lucide/svelte/icons/info";
  import XIcon from "@lucide/svelte/icons/x";
  import XCircleIcon from "@lucide/svelte/icons/x-circle";

  function keysOf<T extends object>(obj: T): Array<keyof T> {
    return Object.keys(obj) as Array<keyof T>;
  }

  export const notifyVariants = tv({
    variants: {
      variant: {
        warning: "text-yellow-500",
        success: "text-green-500",
        error: "text-destructive",
        info: "text-blue-500",
      },
    },
  });

  const variantIcon = {
    warning: AlertTriangleIcon,
    success: CheckCircle2Icon,
    error: XCircleIcon,
    info: InfoIcon,
  };

  export const notificationStore = createNotifications(
    keysOf(notifyVariants.variants.variant),
  );
</script>

<script lang="ts">
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";
  import AcerolaPopover from "$lib/components/acerola-popover/acerola-popover.svelte";
  import { Button } from "$lib/components/ui/button";
  import { fly, fade } from "svelte/transition";
  import { flip } from "svelte/animate";
  import { cubicOut } from "svelte/easing";

  const { notifications, pop, clearAll } = notificationStore;
</script>

<AcerolaPopover contentClass="overflow-hidden">
  {#snippet trigger()}
    <AcerolaButtonIcon class="relative">
      <BellIcon size={20} />
      {#if notifications.length > 0}
        <span
          class="absolute top-1.5 right-1.5 size-2 rounded-full bg-primary border-2 border-background"
        ></span>
      {/if}
    </AcerolaButtonIcon>
  {/snippet}

  {#snippet content()}
    <div class="max-h-96 overflow-y-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden flex flex-col gap-2">
      {#if notifications.length === 0}
        <p class="py-6 text-center text-sm text-muted-foreground">
          Nenhuma notificação
        </p>
      {:else}
        {#each notifications as notify (notify.id)}
          {@const Icon = variantIcon[notify.variant]}

          <div
            animate:flip={{ duration: 200 }}
            in:fly={{ x: 16, duration: 250, easing: cubicOut }}
            out:fade={{ duration: 150 }}
            class="flex items-center gap-3 p-2 rounded-lg border border-border bg-card"
          >
            <Icon
              size={18}
              class={notifyVariants({ variant: notify.variant })}
            />

            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-foreground truncate">
                {notify.message}
              </p>
              
              {#if notify.description}
                <p class="text-xs text-muted-foreground truncate">
                  {notify.description}
                </p>
              {/if}
            </div>

            <AcerolaButtonIcon
              class="size-6 shrink-0"
              onclick={() => pop(notify.id)}
            >
              <XIcon size={12} />
            </AcerolaButtonIcon>
          </div>
        {/each}

        <Button
          variant="ghost"
          class="w-full text-xs text-muted-foreground"
          onclick={clearAll}
        >
          Limpar tudo
        </Button>
      {/if}
    </div>
  {/snippet}
</AcerolaPopover>
