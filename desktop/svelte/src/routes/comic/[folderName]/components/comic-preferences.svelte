<script lang="ts" module>
	export type ComicPreferencesProps = {
		data?: {
			hasVolumeStructure?: boolean;
			bookmarks?: { id: number; name: string; color: number }[];
		};
		state: {
			chaptersPerPage: string;
			volumeViewMode: 'cover' | 'banner';
			bookmarkId: number | null;
			externalSyncEnabled: boolean;
		};
		events: {
			onChaptersPerPageChange: (value: string) => void;
			onVolumeViewModeChange: (value: 'cover' | 'banner') => void;
			onBookmarkChange: (value: number | null) => void;
			onExternalSyncChange: (value: boolean) => void;
			onSyncMangadex?: () => void;
			onSyncAnilist?: () => void;
			onSyncComicInfo?: () => void;
		};
	};
</script>

<script lang="ts">
	import AcerolaHeroButton from '$lib/components/acerola-hero-button/acerola-hero-button.svelte';
	import AcerolaSelect from '$lib/components/acerola-select/acerola-select.svelte';
	import AcerolaToggleGroup from '$lib/components/acerola-toggle-group/acerola-toggle-group.svelte';
	import { ToggleGroupItem } from '$lib/components/ui/toggle-group/index.js';
	import { m } from '$lib/paraglide/messages';

	import Hash from '@lucide/svelte/icons/hash';
	import Layers from '@lucide/svelte/icons/layers';
	import Settings2 from '@lucide/svelte/icons/settings-2';
	import BookmarkIcon from '@lucide/svelte/icons/bookmark';
	import RefreshCw from '@lucide/svelte/icons/refresh-cw';
	import CloudSync from '@lucide/svelte/icons/cloud-sync';
	import Link from '@lucide/svelte/icons/link';
	import FileText from '@lucide/svelte/icons/file-text';
	import MangaDexIcon from '$lib/assets/icons/mangadex.svg?component';
	import AniListIcon from '$lib/assets/icons/anilist.svg?component';
	import AcerolaButtonIcon from '$lib/components/acerola-button/acerola-button-icon.svelte';
	import AcerolaSwitch from '$lib/components/acerola-switch/acerola-switch.svelte';

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
			<!-- Volume Highlight (Conditional) -->
			{#if data?.hasVolumeStructure}
				<AcerolaHeroButton
					data={{
						title: m['pages.comic.preferences.volume_highlight.title'](),
						description: m['pages.comic.preferences.volume_highlight.desc']()
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
									{m['pages.comic.preferences.volume_highlight.cover']()}
								</ToggleGroupItem>

								<ToggleGroupItem
									value="banner"
									class="px-4 py-1.5 text-[10px] font-black uppercase"
								>
									{m['pages.comic.preferences.volume_highlight.banner']()}
								</ToggleGroupItem>
							{/snippet}
						</AcerolaToggleGroup>
					{/snippet}
				</AcerolaHeroButton>
			{/if}

			<!-- Chapters per page -->
			<AcerolaHeroButton
				data={{
					title: m['pages.comic.preferences.chapters_per_page.title'](),
					description: m['pages.comic.preferences.chapters_per_page.desc']()
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

			<!-- Bookmark Assignment -->
			<AcerolaHeroButton
				data={{
					title: m['pages.comic.preferences.bookmark.title'](),
					description: m['pages.comic.preferences.bookmark.desc']()
				}}
			>
				{#snippet icon()}
					<BookmarkIcon class="text-chart-4" size={24} />
				{/snippet}

				{#snippet action()}
					<AcerolaSelect
						data={{
							options: [
								{ value: 'none', label: m['pages.comic.preferences.bookmark.none']() },
								...(data?.bookmarks ?? []).map((b) => ({
									value: b.id.toString(),
									label: b.name,
									color: b.color
								}))
							]
						}}
						state={{ value: state.bookmarkId ? state.bookmarkId.toString() : 'none' }}
						events={{
							onValueChange: (v) => events.onBookmarkChange(v === 'none' ? null : parseInt(v))
						}}
					/>
				{/snippet}
			</AcerolaHeroButton>
		</div>
	</section>

	<!-- Metadata Sync Section -->
	<section class="space-y-4">
		<div
			class="flex items-center gap-3 text-xs font-bold tracking-widest text-muted-foreground uppercase"
		>
			<CloudSync size={16} />
			{m['pages.config.metadata.title']()}
		</div>

		<div class="grid gap-4">
			<AcerolaHeroButton
				data={{
					title: m['pages.comic.preferences.external_sync.title'](),
					description: m['pages.comic.preferences.external_sync.desc']()
				}}
			>
				{#snippet icon()}
					<Link class="text-chart-1" size={24} />
				{/snippet}

				{#snippet action()}
					<AcerolaSwitch
						state={{ checked: state.externalSyncEnabled }}
						events={{ onCheckedChange: events.onExternalSyncChange }}
					/>
				{/snippet}
			</AcerolaHeroButton>

			<AcerolaHeroButton
				data={{
					title: m['pages.config.metadata.mangadex.title'](),
					description: m['pages.config.metadata.mangadex.desc']()
				}}
				events={{ onClick: events.onSyncMangadex }}
			>
				{#snippet icon()}
					<span style="all: unset; display: inline-flex;">
						<MangaDexIcon class="h-6 w-6 rounded-lg" />
					</span>
				{/snippet}

				{#snippet action()}
					<AcerolaButtonIcon
						ui={{
							class:
								'rounded-full transition-all group-hover:bg-primary group-hover:text-primary-foreground'
						}}
					>
						<RefreshCw />
					</AcerolaButtonIcon>
				{/snippet}
			</AcerolaHeroButton>

			<AcerolaHeroButton
				data={{
					title: m['pages.config.metadata.anilist.title'](),
					description: m['pages.config.metadata.anilist.desc']()
				}}
				events={{ onClick: events.onSyncAnilist }}
			>
				{#snippet icon()}
					<span style="all: unset; display: inline-flex;">
						<AniListIcon class="h-6 w-6 rounded-lg" />
					</span>
				{/snippet}

				{#snippet action()}
					<AcerolaButtonIcon
						ui={{
							class:
								'rounded-full transition-all group-hover:bg-primary group-hover:text-primary-foreground'
						}}
					>
						<RefreshCw />
					</AcerolaButtonIcon>
				{/snippet}
			</AcerolaHeroButton>

			<AcerolaHeroButton
				data={{
					title: m['pages.comic.toast.comic_info.title'](),
					description: m['pages.comic.toast.comic_info.desc']()
				}}
				events={{ onClick: events.onSyncComicInfo }}
			>
				{#snippet icon()}
					<span style="all: unset; display: inline-flex;">
						<FileText class="h-6 w-6 text-foreground" />
					</span>
				{/snippet}

				{#snippet action()}
					<AcerolaButtonIcon
						ui={{
							class:
								'rounded-full transition-all group-hover:bg-primary group-hover:text-primary-foreground'
						}}
					>
						<RefreshCw />
					</AcerolaButtonIcon>
				{/snippet}
			</AcerolaHeroButton>
		</div>
	</section>
</div>
