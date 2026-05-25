<script lang="ts">
	import Settings2 from '@lucide/svelte/icons/settings-2';
	import Tag from '@lucide/svelte/icons/tag';
	import AcerolaSelect from '$lib/components/acerola-select/acerola-select.svelte';
	import AcerolaToggleGroup from '$lib/components/acerola-toggle-group/acerola-toggle-group.svelte';
	import { ToggleGroupItem } from '$lib/components/ui/toggle-group/index.js';

	let {
		displayMode = $bindable(),
		chaptersPerPage = $bindable(),
		mediaType = $bindable(),
		volumeViewMode = $bindable(),
		hasVolumeStructure = false
	}: {
		displayMode: string;
		chaptersPerPage: string;
		mediaType: string;
		volumeViewMode: 'cover' | 'banner';
		hasVolumeStructure?: boolean;
	} = $props();
</script>

<div class="grid grid-cols-1 gap-8 md:grid-cols-2">
	<section class="space-y-6">
		<h3
			class="text-overlay flex items-center gap-3 text-[10px] font-black tracking-[0.3em] uppercase"
		>
			<Settings2 size={16} /> Leitura
		</h3>
		<div class="space-y-6 rounded-4xl border border-surface/40 bg-mantle/40 p-8">
			<div class="flex items-center justify-between">
				<span class="text-sm font-bold">Modo de Exibição</span>
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

			{#if hasVolumeStructure}
				<div class="flex items-center justify-between border-t border-surface/20 pt-6">
					<span class="text-sm font-bold">Destaque do Volume</span>
					<AcerolaToggleGroup type="single" bind:value={volumeViewMode}>
						{#snippet children()}
							<ToggleGroupItem value="cover" class="px-4 py-1.5 text-[10px] font-black uppercase">
								Capa
							</ToggleGroupItem>

							<ToggleGroupItem value="banner" class="px-4 py-1.5 text-[10px] font-black uppercase">
								Banner
							</ToggleGroupItem>
						{/snippet}
					</AcerolaToggleGroup>
				</div>
			{/if}

			<div class="flex items-center justify-between border-t border-surface/20 pt-6">
				<span class="text-sm font-bold">Capítulos por página</span>
				<AcerolaSelect
					bind:value={chaptersPerPage}
					options={[
						{ value: '25', label: '25' },
						{ value: '50', label: '50' },
						{ value: '100', label: '100' }
					]}
				/>
			</div>
		</div>
	</section>

	<section class="space-y-6">
		<h3
			class="text-overlay flex items-center gap-3 text-[10px] font-black tracking-[0.3em] uppercase"
		>
			<Tag size={16} /> Metadados
		</h3>
		<div class="space-y-6 rounded-4xl border border-surface/40 bg-mantle/40 p-8">
			<div class="flex flex-col gap-4">
				<span class="border-b border-surface pb-3 text-sm font-bold">Tipo de Mídia</span>
				<AcerolaToggleGroup type="single" bind:value={mediaType} class="flex flex-wrap gap-2">
					{#snippet children()}
						{#each ['Manga', 'Hq', 'Novel', 'Webtoon'] as cat}
							<ToggleGroupItem
								value={cat}
								class="rounded-xl px-4 py-2 text-[10px] font-black tracking-widest uppercase"
							>
								{cat}
							</ToggleGroupItem>
						{/each}
					{/snippet}
				</AcerolaToggleGroup>
			</div>
		</div>
	</section>
</div>
