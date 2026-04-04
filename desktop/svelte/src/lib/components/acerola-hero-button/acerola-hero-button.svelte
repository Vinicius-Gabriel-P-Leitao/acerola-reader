<script lang="ts">
  import { cn } from "$lib/utils/cn.utils";
  import type { AcerolaHeroButtonProps as Props } from "./acerola-hero-button.types";

  let {
    title,
    description,
    icon,
    action,
    class: className,
    ...rest
  }: Props = $props();
</script>

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div
  class={cn(
    "p-6 rounded-3xl border border-border bg-card flex items-center justify-between group transition-colors",
    rest.onclick ? "cursor-pointer hover:border-primary/50" : "",
    className,
  )}
  {...rest}
>
  <div class="flex items-center gap-4">
    {#if icon}
      <div
        class="w-12 h-12 bg-muted rounded-2xl flex items-center justify-center text-foreground group-hover:text-primary transition-colors"
      >
        {@render icon()}
      </div>
    {/if}

    <div class="text-left">
      <h3 class="font-bold text-foreground text-lg">{title}</h3>
      {#if description}
        <p class="text-sm text-muted-foreground">{description}</p>
      {/if}
    </div>
  </div>

  {#if action}
    <div class="shrink-0 ml-4">
      {@render action()}
    </div>
  {/if}
</div>
