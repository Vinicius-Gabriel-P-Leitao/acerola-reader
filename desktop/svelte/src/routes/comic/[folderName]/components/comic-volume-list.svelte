<script lang="ts" module>
  export type VolumeChapter = {
    id: string;
    title: string;
    isRead: boolean;
  };

  export type Volume = {
    id: string;
    title: string;
    chapters: VolumeChapter[];
    totalChapters: number;
    hasMore: boolean;
  };
</script>

<script lang="ts">
  import { slide } from "svelte/transition";
  import AcerolaHeroButton from "$lib/components/acerola-hero-button/acerola-hero-button.svelte";
  import Folder from "@lucide/svelte/icons/folder";
  import ChevronDown from "@lucide/svelte/icons/chevron-down";
  import FileText from "@lucide/svelte/icons/file-text";
  import RefreshCw from "@lucide/svelte/icons/refresh-cw";

  let {
    volumes = [],
    loading = false,
    onexpand,
  }: {
    volumes: Volume[];
    loading?: boolean;
    onexpand: (volumeId: string | null) => void;
  } = $props();

  let expandedVolumeId = $state<string | null>(null);

  const toggleVolume = (volumeId: string) => {
    if (expandedVolumeId === volumeId) {
      expandedVolumeId = null;
    } else {
      expandedVolumeId = volumeId;
    }
    onexpand(expandedVolumeId);
  };
</script>

<div class="space-y-4">
  {#if volumes && volumes.length > 0}
    {#each volumes as volume}
      <AcerolaHeroButton
        title={volume.title}
        description="{volume.totalChapters} Capítulos inclusos"
        onclick={() => toggleVolume(volume.id)}
        class="bg-mantle/40 border-surface/40"
      >
        {#snippet icon()}
          <Folder size={28} />
        {/snippet}
        {#snippet action()}
          <div
            class="transition-transform duration-300 {expandedVolumeId === volume.id ? 'rotate-180' : ''}"
          >
            <ChevronDown size={20} />
          </div>
        {/snippet}
      </AcerolaHeroButton>

      {#if expandedVolumeId === volume.id}
        <div
          transition:slide
          class="space-y-2 ml-10 border-l border-surface/30 p-4 pt-0"
        >
          {#if volume.chapters.length > 0}
            {#each volume.chapters as chapter}
              <AcerolaHeroButton
                title={chapter.title}
                class="bg-base/20 border-transparent hover:bg-surface/30"
              >
                {#snippet icon()}
                  <div
                    class="w-10 h-10 rounded-lg flex items-center justify-center {chapter.isRead ? 'bg-surface/50 text-overlay' : 'bg-primary/10 text-primary'}"
                  >
                    <FileText size={18} />
                  </div>
                {/snippet}
              </AcerolaHeroButton>
            {/each}

            {#if loading}
              <div class="flex items-center justify-center py-4">
                <RefreshCw size={24} class="animate-spin text-primary" />
              </div>
            {/if}
          {:else if loading}
            <div class="flex flex-col items-center justify-center py-10 space-y-4 opacity-50">
                <RefreshCw size={32} class="animate-spin text-primary" />
                <p class="font-black uppercase tracking-widest text-[10px]">Carregando capítulos...</p>
            </div>
          {:else}
            <p class="py-4 text-center text-xs opacity-50 uppercase font-black tracking-widest">
                Nenhum capítulo disponível.
            </p>
          {/if}
        </div>
      {/if}
    {/each}
  {:else}
    <div class="py-20 text-center opacity-50 space-y-4 border-2 border-dashed border-surface rounded-3xl">
      <RefreshCw size={48} class="animate-spin text-primary" />
      <p class="font-black uppercase tracking-widest text-sm">
        Nenhum volume indexado ainda.
      </p>
    </div>
  {/if}
</div>
