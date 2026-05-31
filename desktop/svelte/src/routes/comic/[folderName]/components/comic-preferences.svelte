<script lang="ts">
	import AcerolaHeroButton from '$lib/components/acerola-hero-button/acerola-hero-button.svelte';
	import AcerolaSelect from '$lib/components/acerola-select/acerola-select.svelte';
	import AcerolaToggleGroup from '$lib/components/acerola-toggle-group/acerola-toggle-group.svelte';
	import { ToggleGroupItem } from '$lib/components/ui/toggle-group/index.js';
	import { m } from '$lib/paraglide/messages';

	import BookOpen from '@lucide/svelte/icons/book-open';
	import Hash from '@lucide/svelte/icons/hash';
	import LayoutGrid from '@lucide/svelte/icons/layout-grid';
	import Layers from '@lucide/svelte/icons/layers';
	import Settings2 from '@lucide/svelte/icons/settings-2';
	import Tag from '@lucide/svelte/icons/tag';

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

<div class="space-y-12">
	<!-- Reading Section -->
	<section class="space-y-4">
		<div
			class="flex items-center gap-3 text-xs font-bold tracking-widest text-muted-foreground uppercase"
		>
			<Settings2 size={16} />
			{m['pages.comic.preferences.reading']()}
		</div>

		<div class="grid gap-4">
			<!-- Display Mode -->
			<AcerolaHeroButton
				title={m['pages.comic.preferences.display_mode']()}
				description={m['pages.comic.preferences.display_mode_desc']()}
			>
				{#snippet icon()}
					<LayoutGrid class="text-chart-1" size={24} />
				{/snippet}

				{#snippet action()}
					<AcerolaToggleGroup type="single" bind:value={displayMode}>
						{#snippet children()}
							<ToggleGroupItem value="Lista" class="px-4 py-1.5 text-[10px] font-black uppercase">
								{m['pages.comic.preferences.display_mode_list']()}
							</ToggleGroupItem>

							<ToggleGroupItem value="Grade" class="px-4 py-1.5 text-[10px] font-black uppercase">
								{m['pages.comic.preferences.display_mode_grid']()}
							</ToggleGroupItem>
						{/snippet}
					</AcerolaToggleGroup>
				{/snippet}
			</AcerolaHeroButton>

			<!-- Volume Highlight (Conditional) -->
			{#if hasVolumeStructure}
				<AcerolaHeroButton
					title={m['pages.comic.preferences.volume_highlight']()}
					description={m['pages.comic.preferences.volume_highlight_desc']()}
				>
					{#snippet icon()}
						<Layers class="text-chart-2" size={24} />
					{/snippet}

					{#snippet action()}
						<AcerolaToggleGroup type="single" bind:value={volumeViewMode}>
							{#snippet children()}
								<ToggleGroupItem value="cover" class="px-4 py-1.5 text-[10px] font-black uppercase">
									{m['pages.comic.preferences.volume_highlight_cover']()}
								</ToggleGroupItem>

								<ToggleGroupItem value="banner" class="px-4 py-1.5 text-[10px] font-black uppercase">
									{m['pages.comic.preferences.volume_highlight_banner']()}
								</ToggleGroupItem>
							{/snippet}
						</AcerolaToggleGroup>
					{/snippet}				</AcerolaHeroButton>
			{/if}

			<!-- Chapters per page -->
			<AcerolaHeroButton
				title={m['pages.comic.preferences.chapters_per_page']()}
				description={m['pages.comic.preferences.chapters_per_page_desc']()}
			>
				{#snippet icon()}
					<Hash class="text-chart-3" size={24} />
				{/snippet}

				{#snippet action()}
					<AcerolaSelect
						bind:value={chaptersPerPage}
						options={[
							{ value: '25', label: '25' },
							{ value: '50', label: '50' },
							{ value: '100', label: '100' }
						]}
					/>
				{/snippet}
			</AcerolaHeroButton>
		</div>
	</section>

	<!-- Metadata Section -->
	<section class="space-y-4">
		<div
			class="flex items-center gap-3 text-xs font-bold tracking-widest text-muted-foreground uppercase"
		>
			<Tag size={16} />
			{m['pages.comic.preferences.metadata']()}
		</div>

		<div class="grid gap-4">
			<!-- Media Type -->
			<AcerolaHeroButton
				title={m['pages.comic.preferences.media_type']()}
				description={m['pages.comic.preferences.media_type_desc']()}
			>
				{#snippet icon()}
					<BookOpen class="text-chart-4" size={24} />
				{/snippet}

				{#snippet action()}
					<AcerolaToggleGroup type="single" bind:value={mediaType} class="flex flex-wrap gap-2">
						{#snippet children()}
							{#each [{ value: 'Manga', label: m['pages.comic.preferences.media_types.manga']() }, { value: 'Hq', label: m['pages.comic.preferences.media_types.hq']() }, { value: 'Novel', label: m['pages.comic.preferences.media_types.novel']() }, { value: 'Webtoon', label: m['pages.comic.preferences.media_types.webtoon']() }] as cat}
								<ToggleGroupItem
									value={cat.value}
									class="rounded-xl px-4 py-2 text-[10px] font-black tracking-widest uppercase"
								>
									{cat.label}
								</ToggleGroupItem>
							{/each}
						{/snippet}
					</AcerolaToggleGroup>
				{/snippet}
			</AcerolaHeroButton>
		</div>
	</section>
</div>
