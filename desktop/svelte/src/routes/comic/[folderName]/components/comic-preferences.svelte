<script lang="ts" module>
	export type ComicPreferencesProps = {
		data?: {
			hasVolumeStructure?: boolean;
		};
		state: {
			displayMode: string;
			chaptersPerPage: string;
			mediaType: string;
			volumeViewMode: 'cover' | 'banner';
		};
		events: {
			onDisplayModeChange: (value: string) => void;
			onChaptersPerPageChange: (value: string) => void;
			onMediaTypeChange: (value: string) => void;
			onVolumeViewModeChange: (value: 'cover' | 'banner') => void;
		};
	};
</script>

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

	let { data, events, state }: ComicPreferencesProps = $props();
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
				data={{
					title: m['pages.comic.preferences.display_mode'](),
					description: m['pages.comic.preferences.display_mode_desc']()
				}}
			>
				{#snippet icon()}
					<LayoutGrid class="text-chart-1" size={24} />
				{/snippet}

				{#snippet action()}
					<AcerolaToggleGroup
						config={{ type: 'single' }}
						state={{ value: state.displayMode }}
						events={{
							onValueChange: (value) => {
								if (typeof value === 'string') events.onDisplayModeChange(value);
							}
						}}
					>
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
			{#if data?.hasVolumeStructure}
				<AcerolaHeroButton
					data={{
						title: m['pages.comic.preferences.volume_highlight'](),
						description: m['pages.comic.preferences.volume_highlight_desc']()
					}}
				>
					{#snippet icon()}
						<Layers class="text-chart-2" size={24} />
					{/snippet}

					{#snippet action()}
						<AcerolaToggleGroup
							config={{ type: 'single' }}
							state={{ value: state.volumeViewMode }}
							events={{
								onValueChange: (value) => {
									if (value === 'cover' || value === 'banner') {
										events.onVolumeViewModeChange(value);
									}
								}
							}}
						>
							{#snippet children()}
								<ToggleGroupItem value="cover" class="px-4 py-1.5 text-[10px] font-black uppercase">
									{m['pages.comic.preferences.volume_highlight_cover']()}
								</ToggleGroupItem>

								<ToggleGroupItem
									value="banner"
									class="px-4 py-1.5 text-[10px] font-black uppercase"
								>
									{m['pages.comic.preferences.volume_highlight_banner']()}
								</ToggleGroupItem>
							{/snippet}
						</AcerolaToggleGroup>
					{/snippet}
				</AcerolaHeroButton>
			{/if}

			<!-- Chapters per page -->
			<AcerolaHeroButton
				data={{
					title: m['pages.comic.preferences.chapters_per_page'](),
					description: m['pages.comic.preferences.chapters_per_page_desc']()
				}}
			>
				{#snippet icon()}
					<Hash class="text-chart-3" size={24} />
				{/snippet}

				{#snippet action()}
					<AcerolaSelect
						data={{
							options: [
								{ value: '25', label: '25' },
								{ value: '50', label: '50' },
								{ value: '100', label: '100' }
							]
						}}
						state={{ value: state.chaptersPerPage }}
						events={{
							onValueChange: events.onChaptersPerPageChange
						}}
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
				data={{
					title: m['pages.comic.preferences.media_type'](),
					description: m['pages.comic.preferences.media_type_desc']()
				}}
			>
				{#snippet icon()}
					<BookOpen class="text-chart-4" size={24} />
				{/snippet}

				{#snippet action()}
					<AcerolaToggleGroup
						config={{ type: 'single' }}
						state={{ value: state.mediaType }}
						events={{
							onValueChange: (value) => {
								if (typeof value === 'string') events.onMediaTypeChange(value);
							}
						}}
						ui={{ class: 'flex flex-wrap gap-2' }}
					>
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
