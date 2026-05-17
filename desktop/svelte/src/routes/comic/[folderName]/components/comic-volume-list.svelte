<script lang="ts" module>
  export type VolumeChapter = {
    id: string;
    title: string;
    date: string;
    isRead: boolean;
  };

  export type Volume = {
    id: string;
    title: string;
    chapters: VolumeChapter[];
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
  }: {
    volumes: Volume[];
  } = $props();

  let expandedVolumes = $state<string[]>([]);

  const toggleVolume = (volumeId: string) => {
    if (expandedVolumes.includes(volumeId)) {
      expandedVolumes = expandedVolumes.filter((id) => id !== volumeId);
    } else {
      expandedVolumes = [...expandedVolumes, volumeId];
    }
  };
</script>

<div class="space-y-4">
  {#if volumes && volumes.length > 0}
    {#each volumes as volume}
      <AcerolaHeroButton
        title={volume.title}
        description="{volume.chapters.length} Capítulos inclusos"
        onclick={() => toggleVolume(volume.id)}
        class="bg-mantle/40 border-surface/40"
      >
        {#snippet icon()}
          <Folder size={28} />
        {/snippet}
        {#snippet action()}
          <div
            class="transition-transform duration-300 {expandedVolumes.includes(volume.id) ? 'rotate-180' : ''}"
          >
            <ChevronDown size={20} />
          </div>
        {/snippet}
      </AcerolaHeroButton>

      {#if expandedVolumes.includes(volume.id)}
        <div
          transition:slide
          class="space-y-2 ml-10 border-l border-surface/30 p-4 pt-0"
        >
          {#each volume.chapters as chapter}
            <AcerolaHeroButton
              title={chapter.title}
              description={chapter.date}
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
        </div>
      {/if}
    {/each}
  {:else}
    <div class="py-20 text-center opacity-50 space-y-4 border-2 border-dashed border-surface rounded-3xl">
      <RefreshCw size={48} class="animate-spin mx-auto text-primary" />
      <p class="font-black uppercase tracking-widest text-sm">
        Nenhum volume indexado ainda.
      </p>
    </div>
  {/if}
</div>
