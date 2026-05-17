<script lang="ts">
  import BookOpen from "@lucide/svelte/icons/book-open";
  import Star from "@lucide/svelte/icons/star";
  import List from "@lucide/svelte/icons/list";
  import { ScrollArea } from "$lib/components/ui/scroll-area/index.js";

  let {
    banner,
    genres = [],
  }: {
    banner?: string | null;
    genres?: string[];
  } = $props();
</script>

<div class="relative w-full h-75 lg:h-112.5 shrink-0">
  {#if banner}
    <div class="w-full h-full relative">
      <img
        src={banner}
        alt="Banner"
        class="w-full h-full object-cover"
        referrerpolicy="no-referrer"
      />
      <div
        class="absolute inset-0 bg-linear-to-t from-base via-base/20 to-transparent"
      ></div>
      <div
        class="absolute inset-0 bg-linear-to-l from-transparent via-transparent to-base/40 hidden lg:block"
      ></div>
    </div>
  {:else}
    <div
      class="w-full h-full bg-linear-to-b from-primary/20 via-base/50 to-base"
    ></div>
  {/if}

  <!-- Floating stats and Genres on Banner -->
  <div class="absolute bottom-6 left-6 lg:left-16 right-6 flex flex-col gap-4">
    <div class="flex gap-4 lg:gap-6 shrink-0">
      <div
        class="flex items-center gap-3 bg-surface/20 backdrop-blur-xl border border-surface/30 px-6 py-3 rounded-2xl"
      >
        <Star size={20} class="text-yellow-400 fill-yellow-400" />
        <span class="font-black text-xl tracking-tighter">9.8</span>
      </div>

      <div
        class="flex items-center gap-3 bg-surface/20 backdrop-blur-xl border border-surface/30 px-6 py-3 rounded-2xl"
      >
        <List size={20} class="text-primary" />
        <span class="font-black text-xl tracking-tighter">Popular</span>
      </div>
    </div>

    {#if genres.length > 0}
      <ScrollArea orientation="horizontal" class="w-full whitespace-nowrap" scrollbarXClasses="h-1.5 opacity-0 group-hover:opacity-100 transition-opacity">
        <div class="flex gap-2 pb-3 pr-6">
          {#each genres as genre}
            <span class="whitespace-nowrap px-4 py-1.5 bg-surface/40 backdrop-blur-xl border border-surface/30 rounded-full text-[10px] font-black uppercase tracking-widest text-text shadow-sm">{genre}</span>
          {/each}
        </div>
      </ScrollArea>
    {/if}
  </div>
</div>
