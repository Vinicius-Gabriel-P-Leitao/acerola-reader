<script lang="ts">
  import AcerolaButton from "$lib/components/acerola-button/acerola-button.svelte";
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";
  import ArrowLeft from "@lucide/svelte/icons/arrow-left";
  import Play from "@lucide/svelte/icons/play";
  import Bookmark from "@lucide/svelte/icons/bookmark";
  import RefreshCw from "@lucide/svelte/icons/refresh-cw";

  let {
    title,
    author,
    category,
    chaptersCount,
    description,
    cover,
    onBack,
  }: {
    title: string;
    author: string;
    category: string;
    chaptersCount: number;
    description: string;
    cover: string;
    onBack: () => void;
  } = $props();
</script>

<div class="hidden lg:flex w-100 flex-col h-full bg-mantle/60 border-r border-surface/30 backdrop-blur-3xl relative z-10 shrink-0 select-none">
  <div class="p-10 flex flex-col h-full">
    <AcerolaButtonIcon onclick={onBack} class="mb-10 shadow-lg group">
      <ArrowLeft size={24} class="group-hover:-translate-x-1 transition-transform" />
    </AcerolaButtonIcon>

    <div class="flex-1 flex flex-col items-center text-center space-y-8">
      <div class="w-64 aspect-2/3 rounded-huge overflow-hidden shadow-2xl border-4 border-surface shrink-0">
        <img src={cover} alt={title} class="w-full h-full object-cover" referrerpolicy="no-referrer" />
      </div>

      <div class="space-y-3 px-4">
        <h1 class="text-4xl font-black tracking-tighter leading-tight">{title}</h1>
        <p class="text-lg text-primary font-bold">{author}</p>
        <div class="flex flex-wrap justify-center gap-2 pt-2">
          <span class="px-3 py-1 bg-surface/50 rounded-lg text-[10px] font-black uppercase tracking-widest text-subtext">{category}</span>
          <span class="px-3 py-1 bg-surface/50 rounded-lg text-[10px] font-black uppercase tracking-widest text-subtext">{chaptersCount} Caps</span>
        </div>
      </div>

      <div class="w-full bg-base/30 border border-surface/30 rounded-3xl p-6 text-left space-y-3">
        <h3 class="text-[10px] font-black uppercase tracking-widest text-overlay">Sinopse</h3>
        <p class="text-xs text-subtext leading-relaxed line-clamp-6 font-medium">{description}</p>
      </div>

      <div class="w-full pt-4 space-y-3">
        <AcerolaButton class="w-full py-8 rounded-3xl font-black flex items-center justify-center gap-3 shadow-xl shadow-primary/20">
          <Play size={24} fill="currentColor" /> LER AGORA
        </AcerolaButton>
        <div class="grid grid-cols-2 gap-3">
          <AcerolaButton variant="outline" class="py-6 rounded-2xl font-black flex items-center justify-center gap-2 text-xs">
            <Bookmark size={16} /> SALVAR
          </AcerolaButton>
          <AcerolaButton variant="outline" class="py-6 rounded-2xl font-black flex items-center justify-center gap-2 text-xs">
            <RefreshCw size={16} /> SYNC
          </AcerolaButton>
        </div>
      </div>
    </div>
  </div>
</div>
