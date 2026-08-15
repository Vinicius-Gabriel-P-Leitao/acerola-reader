<script lang="ts" module>
	export type ComicPreferencesProps = {
		data?: {
			hasVolumeStructure?: boolean;
			bookmarks?: { id: number; name: string; color: number }[];
		};
		state: {
			volumeViewMode: 'cover' | 'banner';
			bookmarkId: number | null;
			externalSyncEnabled: boolean;
		};
		events: {
			onVolumeViewModeChange: (value: 'cover' | 'banner') => void;
			onBookmarkChange: (value: number | null) => void;
			onExternalSyncChange: (value: boolean) => void;
			onSyncMangadex?: () => void;
			onSyncAnilist?: () => void;
			onSyncComicInfo?: () => void;
			onRescanComic?: () => void;
			onDeepRescanComic?: () => void;
			onRegenerateCover?: () => void;
			onRegenerateVolumeCovers?: () => void;
			onClearMetadata?: () => Promise<void> | void;
		};
	};
</script>

<script lang="ts">
	import AcerolaHeroButton from '$lib/components/acerola-hero-button/acerola-hero-button.svelte';
	import AcerolaSelect from '$lib/components/acerola-select/acerola-select.svelte';
	import AcerolaToggleGroup from '$lib/components/acerola-toggle-group/acerola-toggle-group.svelte';
	import AcerolaAlertDialog from '$lib/components/acerola-alert-dialog/acerola-alert-dialog.svelte';
	import { ToggleGroupItem } from '$lib/components/ui/toggle-group/index.js';
	import { m } from '$lib/paraglide/messages';

	import Layers from '@lucide/svelte/icons/layers';
	import Settings2 from '@lucide/svelte/icons/settings-2';
	import BookmarkIcon from '@lucide/svelte/icons/bookmark';
	import RefreshCw from '@lucide/svelte/icons/refresh-cw';
	import CloudSync from '@lucide/svelte/icons/cloud-sync';
	import Link from '@lucide/svelte/icons/link';
	import FileText from '@lucide/svelte/icons/file-text';
	import MangaDexIcon from '$lib/assets/icons/mangadex.svg?component';
	import AniListIcon from '$lib/assets/icons/anilist.svg?component';
	import FolderSync from '@lucide/svelte/icons/folder-sync';
	import DatabaseZap from '@lucide/svelte/icons/database-zap';
	import Image from '@lucide/svelte/icons/image';
	import Layers2 from '@lucide/svelte/icons/layers-2';
	import Eraser from '@lucide/svelte/icons/eraser';
	import AcerolaButtonIcon from '$lib/components/acerola-button/acerola-button-icon.svelte';
	import AcerolaSwitch from '$lib/components/acerola-switch/acerola-switch.svelte';

	let { data, events, state: preferences }: ComicPreferencesProps = $props();

	let showClearMetadataDialog = $state(false);
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
							state={{ value: preferences.volumeViewMode }}
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
						state={{ value: preferences.bookmarkId ? preferences.bookmarkId.toString() : 'none' }}
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
						state={{ checked: preferences.externalSyncEnabled }}
						events={{ onCheckedChange: events.onExternalSyncChange }}
					/>
				{/snippet}
			</AcerolaHeroButton>

			{#if preferences.externalSyncEnabled}
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
			{/if}

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

	<!-- File Sync Section -->
	<section class="space-y-4">
		<div
			class="flex items-center gap-3 text-xs font-bold tracking-widest text-muted-foreground uppercase"
		>
			<FolderSync size={16} />
			{m['pages.comic.preferences.file_sync.title']()}
		</div>

		<div class="grid gap-4">
			<AcerolaHeroButton
				data={{
					title: m['pages.comic.preferences.file_sync.rescan.title'](),
					description: m['pages.comic.preferences.file_sync.rescan.desc']()
				}}
				events={{ onClick: events.onRescanComic }}
			>
				{#snippet icon()}
					<FolderSync class="text-chart-1" size={24} />
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
					title: m['pages.comic.preferences.file_sync.deep_rescan.title'](),
					description: m['pages.comic.preferences.file_sync.deep_rescan.desc']()
				}}
				events={{ onClick: events.onDeepRescanComic }}
			>
				{#snippet icon()}
					<DatabaseZap class="text-destructive" size={24} />
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

	<!-- Cover Section -->
	<section class="space-y-4">
		<div
			class="flex items-center gap-3 text-xs font-bold tracking-widest text-muted-foreground uppercase"
		>
			<Image size={16} />
			{m['pages.comic.preferences.cover.title']()}
		</div>

		<div class="grid gap-4">
			<AcerolaHeroButton
				data={{
					title: m['pages.comic.preferences.cover.regenerate.title'](),
					description: m['pages.comic.preferences.cover.regenerate.desc']()
				}}
				events={{ onClick: events.onRegenerateCover }}
			>
				{#snippet icon()}
					<Image class="text-chart-2" size={24} />
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

			{#if data?.hasVolumeStructure}
				<AcerolaHeroButton
					data={{
						title: m['pages.comic.preferences.cover.regenerate_volumes.title'](),
						description: m['pages.comic.preferences.cover.regenerate_volumes.desc']()
					}}
					events={{ onClick: events.onRegenerateVolumeCovers }}
				>
					{#snippet icon()}
						<Layers2 class="text-chart-3" size={24} />
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
			{/if}
		</div>
	</section>

	<!-- Danger Zone -->
	<section class="space-y-4">
		<div
			class="flex items-center gap-3 text-xs font-bold tracking-widest text-muted-foreground uppercase"
		>
			<Eraser size={16} />
			{m['pages.comic.preferences.danger_zone.title']()}
		</div>

		<div class="grid gap-4">
			<AcerolaHeroButton
				data={{
					title: m['pages.comic.preferences.danger_zone.clear_metadata.title'](),
					description: m['pages.comic.preferences.danger_zone.clear_metadata.desc']()
				}}
				events={{ onClick: () => (showClearMetadataDialog = true) }}
			>
				{#snippet icon()}
					<Eraser class="text-destructive" size={24} />
				{/snippet}
			</AcerolaHeroButton>
		</div>
	</section>
</div>

<AcerolaAlertDialog
	state={{ open: showClearMetadataDialog }}
	data={{
		title: m['pages.comic.preferences.danger_zone.clear_metadata.confirm.title'](),
		description: m['pages.comic.preferences.danger_zone.clear_metadata.confirm.desc'](),
		cancelText: m['pages.comic.preferences.danger_zone.clear_metadata.confirm.cancel'](),
		actionText: m['pages.comic.preferences.danger_zone.clear_metadata.confirm.action']()
	}}
	events={{
		onAction: async () => {
			await events.onClearMetadata?.();
			showClearMetadataDialog = false;
		},
		onCancel: () => (showClearMetadataDialog = false)
	}}
	ui={{ variant: 'destructive' }}
/>
