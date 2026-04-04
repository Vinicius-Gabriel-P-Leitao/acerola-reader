<script lang="ts">
  import { m } from "$lib/paraglide/messages";
  import { cn } from "$lib/utils/cn.utils";
  import type { AcerolaHeroButtonProps } from "./acerola-hero-button.types";

  let {
    title,
    description,
    class: className,
    icon,
    action,
    onclick,
    ...rest
  }: AcerolaHeroButtonProps = $props();
</script>

<div
  class={cn(
    "p-6 rounded-3xl border border-border bg-card flex items-center justify-between group transition-colors",
    onclick ? "cursor-pointer hover:border-primary/50" : "",
    className,
  )}
  {onclick}
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
      <h3 class="font-bold text-foreground text-lg">
        {title ?? m["components.hero_button.default_title"]()}
      </h3>
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
