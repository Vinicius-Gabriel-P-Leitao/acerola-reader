<script lang="ts">
  import BookOpenIcon from "@lucide/svelte/icons/book-open";
  import { AspectRatio } from "$lib/components/ui/aspect-ratio";
  import { cn } from "$lib/utils/cn.utils";
  import type { AcerolaCardImageProps } from "./acerola-card-image.types";

  let {
    title,
    cover,
    progress,
    description,
    placeholder,
    footer,
    action,
    overlay,
    class: className,
  }: AcerolaCardImageProps = $props();
</script>

<div
  class={cn(
    "group cursor-pointer w-36 transition-transform duration-300 hover:-translate-y-2 hover:scale-[1.02]",
    className,
  )}
>
  <!-- Imagem -->
  <AspectRatio
    ratio={2 / 3}
    class="rounded-3xl overflow-hidden bg-surface shadow-lg group-hover:shadow-2xl group-hover:shadow-primary/20 transition-shadow duration-300"
  >
    <div class="relative w-full h-full">
      {#if cover}
        <img
          src={cover}
          alt={title}
          class="w-full h-full object-cover object-top transition-transform duration-700 group-hover:scale-110"
        />
      {:else if placeholder}
        {@render placeholder()}
      {:else}
        <div
          class="w-full h-full flex items-center justify-center bg-surface text-muted-foreground"
        >
          <BookOpenIcon size={40} />
        </div>
      {/if}

      <!-- Overlay hover -->
      <div
        class="absolute inset-0 bg-linear-to-t from-crust via-crust/30 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex flex-col justify-end p-4"
      >
        {#if overlay}
          {@render overlay()}
        {:else if description}
          <p
            class="text-foreground text-xs line-clamp-3 translate-y-2 group-hover:translate-y-0 transition-transform duration-300"
          >
            {description}
          </p>
        {/if}
      </div>

      <!-- Barra de progresso -->
      {#if progress !== undefined}
        <div class="absolute bottom-0 left-0 w-full h-1 bg-surface">
          <div
            class="h-full bg-primary transition-all duration-500"
            style="width: {Math.min(100, Math.max(0, progress))}%"
          ></div>
        </div>
      {/if}
    </div>
  </AspectRatio>

  <!-- Info abaixo da imagem -->
  <div class="mt-3 px-1 flex items-start justify-between gap-2">
    <div class="min-w-0 flex-1">
      <h3
        class="font-bold text-sm line-clamp-1 group-hover:text-primary transition-colors duration-200"
      >
        {title}
      </h3>
      {#if footer}
        <div class="mt-1">
          {@render footer()}
        </div>
      {/if}
    </div>

    {#if action}
      {@render action()}
    {/if}
  </div>
</div>
