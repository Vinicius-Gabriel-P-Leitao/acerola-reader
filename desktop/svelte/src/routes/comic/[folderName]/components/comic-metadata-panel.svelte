<script lang="ts">
  import AcerolaButton from "$lib/components/acerola-button/acerola-button.svelte";
  import AcerolaButtonIcon from "$lib/components/acerola-button/acerola-button-icon.svelte";
  import AcerolaCardImage from "$lib/components/acerola-card/acerola-card-image.svelte";
  import PlaceholderManga from "$lib/assets/placeholder/placeholder_manga.svg?component";
  import ArrowLeft from "@lucide/svelte/icons/arrow-left";
  import Play from "@lucide/svelte/icons/play";
  import Bookmark from "@lucide/svelte/icons/bookmark";
  import RefreshCw from "@lucide/svelte/icons/refresh-cw";

  let {
    title,
    author,
    status,
    source,
    chaptersCount,
    description,
    cover,
    onBack,
  }: {
    title: string;
    author: string;
    status: string;
    source: string;
    chaptersCount: number;
    description: string;
    cover: string | null;
    onBack: () => void;
  } = $props();
</script>

<div class="hidden lg:flex w-100 flex-col h-full bg-mantle/60 border-r border-surface/30 backdrop-blur-3xl relative z-10 shrink-0 select-none">
  <div class="p-10 flex flex-col h-full">
    <AcerolaButtonIcon onclick={onBack} class="mb-10 shadow-lg group">
      <ArrowLeft size={24} class="group-hover:-translate-x-1 transition-transform" />
    </AcerolaButtonIcon>

    <div class="flex-1 flex flex-col items-center text-center space-y-6 overflow-y-auto scrollbar-hide">
      <AcerolaCardImage
        data={{
          title: title,
          cover: cover,
        }}
        ui={{
          class: "w-64 shrink-0 [&_.mt-3]:hidden",
        }}
      >
        {#snippet placeholder()}
          <div class="w-full h-full bg-surface">
            <PlaceholderManga class="w-full h-full" />
          </div>
        {/snippet}
      </AcerolaCardImage>

      <div class="space-y-3 px-4 w-full">
        <div class="space-y-1">
          <h1 class="text-4xl font-black tracking-tighter leading-tight line-clamp-2">{title}</h1>
          <p class="text-lg text-primary font-bold">{author}</p>
        </div>
        
        <div class="flex flex-wrap justify-center gap-2">
          <span class="px-4 py-1.5 bg-surface/80 border border-surface/30 rounded-lg text-[10px] font-black uppercase tracking-widest text-text shadow-sm">{status}</span>
          <span class="px-4 py-1.5 bg-surface/80 border border-surface/30 rounded-lg text-[10px] font-black uppercase tracking-widest text-text shadow-sm">{source}</span>
        </div>
      </div>

      <div class="w-full bg-base/30 border border-surface/30 rounded-3xl p-6 text-left space-y-3">
        <div class="flex items-center justify-between">
          <h3 class="text-[10px] font-black uppercase tracking-widest text-overlay">Sinopse</h3>
          <span class="text-[9px] font-black uppercase tracking-widest text-primary/60">{chaptersCount} Caps</span>
        </div>
        <p class="text-xs text-subtext leading-relaxed line-clamp-6 font-medium">{description}</p>
      </div>

      <div class="w-full pt-4 space-y-3 mt-auto">
        <AcerolaButton class="w-full py-8 rounded-3xl font-black flex items-center justify-center gap-3">
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
