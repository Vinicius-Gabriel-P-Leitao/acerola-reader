<script lang="ts">
  import Settings2 from "@lucide/svelte/icons/settings-2";
  import Tag from "@lucide/svelte/icons/tag";
  import AcerolaSelect from "$lib/components/acerola-select/acerola-select.svelte";
  import AcerolaToggleGroup from "$lib/components/acerola-toggle-group/acerola-toggle-group.svelte";
  import { ToggleGroupItem } from "$lib/components/ui/toggle-group/index.js";

  let {
    displayMode = $bindable(),
    chaptersPerPage = $bindable(),
    mediaType = $bindable(),
  }: {
    displayMode: string;
    chaptersPerPage: string;
    mediaType: string;
  } = $props();
</script>

<div class="grid grid-cols-1 md:grid-cols-2 gap-8">
  <section class="space-y-6">
    <h3 class="flex items-center gap-3 text-overlay uppercase text-[10px] font-black tracking-[0.3em]">
      <Settings2 size={16} /> Leitura
    </h3>
    <div class="bg-mantle/40 border border-surface/40 rounded-4xl p-8 space-y-6">
      <div class="flex items-center justify-between">
        <span class="font-bold text-sm">Modo de Exibição</span>
        <AcerolaToggleGroup type="single" bind:value={displayMode}>
          {#snippet children()}
            <ToggleGroupItem value="Lista" class="px-4 py-1.5 text-[10px] font-black uppercase">
              Lista
            </ToggleGroupItem>
            <ToggleGroupItem value="Grade" class="px-4 py-1.5 text-[10px] font-black uppercase">
              Grade
            </ToggleGroupItem>
          {/snippet}
        </AcerolaToggleGroup>
      </div>
      <div class="flex items-center justify-between">
        <span class="font-bold text-sm">Capítulos por página</span>
        <AcerolaSelect
          bind:value={chaptersPerPage}
          options={[
            { value: "25", label: "25" },
            { value: "50", label: "50" },
            { value: "100", label: "100" },
          ]}
        />
      </div>
    </div>
  </section>

  <section class="space-y-6">
    <h3 class="flex items-center gap-3 text-overlay uppercase text-[10px] font-black tracking-[0.3em]">
      <Tag size={16} /> Metadados
    </h3>
    <div class="bg-mantle/40 border border-surface/40 rounded-4xl p-8 space-y-6">
      <div class="flex flex-col gap-4">
        <span class="font-bold text-sm border-b border-surface pb-3">Tipo de Mídia</span>
        <AcerolaToggleGroup type="single" bind:value={mediaType} class="flex flex-wrap gap-2">
          {#snippet children()}
            {#each ["Manga", "Hq", "Novel", "Webtoon"] as cat}
              <ToggleGroupItem value={cat} class="px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest">
                {cat}
              </ToggleGroupItem>
            {/each}
          {/snippet}
        </AcerolaToggleGroup>
      </div>
    </div>
  </section>
</div>
