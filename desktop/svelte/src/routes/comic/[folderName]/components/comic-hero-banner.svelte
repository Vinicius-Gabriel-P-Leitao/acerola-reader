<script lang="ts">
  import BookOpen from "@lucide/svelte/icons/book-open";
  import Star from "@lucide/svelte/icons/star";
  import List from "@lucide/svelte/icons/list";

  let {
    banner,
  }: {
    banner?: string | null;
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

  <!-- Floating stats on Banner -->
  <div class="absolute bottom-10 left-10 lg:left-16 flex gap-6">
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
</div>
