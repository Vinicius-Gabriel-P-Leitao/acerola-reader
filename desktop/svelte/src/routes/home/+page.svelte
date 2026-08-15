<script lang="ts">
	import { goto } from '$app/navigation';
	import PlaceholderManga from '$lib/assets/placeholder/placeholder_manga.svg?component';
	import AcerolaButton from '$lib/components/acerola-button/acerola-button.svelte';
	import AcerolaButtonIcon from '$lib/components/acerola-button/acerola-button-icon.svelte';
	import AcerolaCardImage from '$lib/components/acerola-card/acerola-card-image.svelte';
	import AcerolaBookmarkRibbon from '$lib/components/acerola-bookmark-ribbon/acerola-bookmark-ribbon.svelte';
	import AcerolaComicActionDialog from './components/acerola-comic-action-dialog.svelte';
	import AcerolaFilterPanel, { type BookmarkFilter } from './components/acerola-filter-panel.svelte';
	import { useBookmarks } from '$lib/hooks/store/use-bookmarks.svelte';
	import { useComicSelection } from '$lib/hooks/store/use-comic-selection.svelte';
	import { useSelectFolder } from '$lib/hooks/store/use-select-folder.svelte';
	import MoreVertical from '@lucide/svelte/icons/more-vertical';
	import BookOpen from '@lucide/svelte/icons/book-open';
	import Check from '@lucide/svelte/icons/check';
	import SlidersHorizontal from '@lucide/svelte/icons/sliders-horizontal';
	import FolderPlus from '@lucide/svelte/icons/folder-plus';
	import RefreshCw from '@lucide/svelte/icons/refresh-cw';
	import SearchX from '@lucide/svelte/icons/search-x';
	import { LIBRARY_EVENTS } from '$lib/contracts/library/library.events';
	import { DIRECTORY_SCAN_COMMANDS } from '$lib/contracts/library/library.commands';
	import { useLibraryScanner } from '$lib/hooks/store/use-comic-scanner.svelte';
	import { useComicSummary } from '$lib/hooks/store/use-comic-summary.svelte';
	import { useComicContext } from '$lib/state/comic-context.svelte';
	import { resolveCover } from '$lib/utils/artwork.utils';
	import { listen } from '@tauri-apps/api/event';
	import { onDestroy, onMount } from 'svelte';
	import { m } from '$lib/paraglide/messages';
	import { toast } from 'svelte-sonner';
	import type { ComicSummaryItemPayload, MetadataSource, SortBy, SortOrder } from '$lib/contracts/home/home.payloads';

	const summary = useComicSummary();
	const activeComic = useComicContext();
	const bookmarkStore = useBookmarks();
	const selection = useComicSelection();
	const folderStore = useSelectFolder();

	const refreshScanner = useLibraryScanner(
		DIRECTORY_SCAN_COMMANDS.refreshLibrary,
		() => folderStore.folderPath
	);

	let unlistenScan: (() => void) | undefined;
	let showFilterPanel = $state(false);
	let showActionDialog = $state(false);
	let bookmarkFilter = $state<BookmarkFilter>('all');

	onMount(async () => {
		await folderStore.loadSavedPath();
		await bookmarkStore.loadBookmarks();
		unlistenScan = await listen(LIBRARY_EVENTS.scanComplete, async () => {
			await summary.fetch();
		});

		await summary.fetch();
	});

	onDestroy(() => {
		unlistenScan?.();
	});

	async function handleHide(ids: (string | number)[]) {
		const validIds = ids.filter((id) => id != null && String(id).trim() !== '');
		if (validIds.length === 0) return;
		const count = await summary.updateVisibility(validIds, true);
		toast.success(m['pages.home.toast.hidden']({ count }));
		selection.exitSelectionMode();
		showActionDialog = false;
	}

	async function handleDelete(ids: (string | number)[]) {
		const validIds = ids.filter((id) => id != null && String(id).trim() !== '');
		if (validIds.length === 0) return;
		const count = await summary.deleteComics(validIds);
		toast.success(m['pages.home.toast.deleted']({ count }));
		selection.exitSelectionMode();
		showActionDialog = false;
	}

	async function handleClearMetadata(ids: (string | number)[]) {
		const validIds = ids.filter((id) => id != null && String(id).trim() !== '');
		if (validIds.length === 0) return;
		const count = await summary.clearMetadata(validIds);
		toast.success(m['pages.home.toast.metadata_cleared']({ count }));
		selection.exitSelectionMode();
		showActionDialog = false;
	}

	async function handleBookmark(ids: (string | number)[], categoryId: number) {
		const validIds = ids.filter((id) => id != null && String(id).trim() !== '');
		if (validIds.length === 0) return;
		let successCount = 0;
		let failCount = 0;
		for (const id of validIds) {
			try {
				await bookmarkStore.assignToComic(id, categoryId);
				successCount++;
			} catch (err) {
				failCount++;
				console.error(`Failed to assign bookmark to comic ${id}:`, err);
			}
		}
		if (successCount > 0) {
			toast.success(m['pages.home.toast.bookmarked']({ count: successCount }));
			await summary.fetch();
		}
		if (failCount > 0) {
			toast.error(m['pages.home.toast.error.bookmark']());
			await summary.fetch();
		}
		selection.exitSelectionMode();
		showActionDialog = false;
	}

	function handleCardClick(comic: ComicSummaryItemPayload, cover: string | null) {
		if (selection.isSelectionMode) {
			selection.toggleSelection(comic.relations.directoryId);
		} else {
			activeComic.set(comic, cover);
			goto(`/comic/${comic.filesystem.folderName}`);
		}
	}

	function handleActionClick(event: MouseEvent, comicId: string | number) {
		event.stopPropagation();
		selection.toggleSelection(comicId);
	}

	function handleSelectAllToggle() {
		const allIds = visibleComics.map((comicItem) => comicItem.relations.directoryId);
		if (selection.selectedCount === allIds.length && allIds.length > 0) {
			selection.deselectAll();
		} else {
			selection.selectAll(allIds);
		}
	}

	function handleFilterApply(params: {
		sortBy: SortBy;
		sortOrder: SortOrder;
		showHidden: boolean;
		metadataSource: MetadataSource;
		bookmarkFilter: BookmarkFilter;
	}) {
		summary.setSorting(params.sortBy, params.sortOrder);
		summary.setFilters(params.showHidden, params.metadataSource);
		bookmarkFilter = params.bookmarkFilter;
		summary.fetch();
		showFilterPanel = false;
	}

	const activeFiltersCount = $derived(
		(summary.showHidden ? 1 : 0) +
			(summary.metadataSource !== 'all' ? 1 : 0) +
			(bookmarkFilter !== 'all' ? 1 : 0)
	);

	const visibleComics = $derived(
		bookmarkFilter === 'all'
			? (summary.comics?.comics ?? [])
			: bookmarkFilter === 'none'
				? (summary.comics?.comics.filter((comic) => comic.bookmark == null) ?? [])
				: (summary.comics?.comics.filter((comic) => comic.bookmark?.id === bookmarkFilter) ?? [])
	);
</script>

{#if summary.loading && (!summary.comics || summary.comics.total === 0)}
	<div class="flex items-center justify-center p-8 text-muted-foreground">
		{m['pages.home.loading']()}
	</div>
{:else if summary.comics && summary.comics.total > 0}
	<div class="px-8 pt-8 pb-8">
		<div class="mb-4 flex items-center justify-between">
			{#if selection.isSelectionMode}
				<div class="flex items-center gap-2">
					<span class="text-sm font-medium text-muted-foreground">
						{m['pages.home.selection.selected']({ count: selection.selectedCount })}
					</span>
					<AcerolaButton
						ui={{ variant: 'ghost', size: 'sm', class: 'rounded-lg' }}
						events={{ onClick: handleSelectAllToggle }}
					>
						{selection.selectedCount === visibleComics.length
							? m['pages.home.selection.all.deselect']()
							: m['pages.home.selection.all.select']()}
					</AcerolaButton>

					<AcerolaButton
						ui={{ variant: 'secondary', size: 'sm', class: 'rounded-lg font-semibold gap-1.5' }}
						events={{ onClick: () => (showActionDialog = true) }}
					>
						<SlidersHorizontal size={14} />
						{m['pages.home.selection.actions_button']({ count: selection.selectedCount })}
					</AcerolaButton>

					<AcerolaButton
						ui={{ variant: 'ghost', size: 'sm', class: 'rounded-lg' }}
						events={{ onClick: () => selection.exitSelectionMode() }}
					>
						{m['pages.home.selection.cancel']()}
					</AcerolaButton>
				</div>
			{:else}
				<div class="flex items-center gap-2">
					<!-- Filter & Sort Button -->
					<AcerolaButton
						ui={{ variant: 'ghost', class: 'rounded-xl gap-2' }}
						events={{ onClick: () => (showFilterPanel = !showFilterPanel) }}
					>
						<svg
							xmlns="http://www.w3.org/2000/svg"
							width="16"
							height="16"
							viewBox="0 0 24 24"
							fill="none"
							stroke="currentColor"
							stroke-width="2.5"
							stroke-linecap="round"
							stroke-linejoin="round"
						>
							<line x1="21" y1="4" x2="14" y2="4"></line>
							<line x1="10" y1="4" x2="3" y2="4"></line>
							<line x1="21" y1="12" x2="12" y2="12"></line>
							<line x1="8" y1="12" x2="3" y2="12"></line>
							<line x1="21" y1="20" x2="16" y2="20"></line>
							<line x1="12" y1="20" x2="3" y2="20"></line>
							<line x1="14" y1="2" x2="14" y2="6"></line>
							<line x1="8" y1="10" x2="8" y2="14"></line>
							<line x1="16" y1="18" x2="16" y2="22"></line>
						</svg>
						{m['pages.home.filter_button']()}
						{#if activeFiltersCount > 0}
							<span
								class="flex h-5 min-w-5 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-black text-primary-foreground"
							>
								{activeFiltersCount}
							</span>
						{/if}
					</AcerolaButton>

					<!-- Active sort indicator -->
					{#if summary.sortBy !== 'title' || summary.sortOrder !== 'asc'}
						<span
							class="rounded-lg bg-primary/15 px-2.5 py-1 text-[11px] font-semibold text-primary"
						>
							{summary.sortBy === 'title'
								? m['pages.home.sort.indicator.title']()
								: summary.sortBy === 'chapterCount'
									? m['pages.home.sort.indicator.chapter_count']()
									: m['pages.home.sort.indicator.last_updated']()}
							{summary.sortOrder === 'asc' ? '↑' : '↓'}
						</span>
					{/if}
				</div>
			{/if}
		</div>

		{#if visibleComics.length === 0}
			<div
				class="flex min-h-[40vh] flex-col items-center justify-center p-12 text-center animate-in fade-in-50 duration-300"
			>
				<div
					class="mb-4 flex size-16 items-center justify-center rounded-2xl bg-surface/60 text-primary shadow-inner"
				>
					<SearchX size={32} />
				</div>
				<h3 class="text-xl font-bold tracking-tight text-foreground">
					{m['pages.home.no_results_filtered_title']()}
				</h3>
				<p class="mt-1.5 max-w-md text-sm text-muted-foreground">
					{m['pages.home.no_results_filtered']()}
				</p>
			</div>
		{/if}

		<div class="grid grid-cols-[repeat(auto-fill,minmax(9rem,1fr))] gap-6">
			{#each visibleComics as comic (comic.relations.directoryId)}
				{@const cover = resolveCover(comic.artwork)}
				{@const bookmarkColor = comic.bookmark?.color}
				{@const bookmarkName = comic.bookmark?.name}
				{@const isSelected = selection.isSelected(comic.relations.directoryId)}
				<AcerolaCardImage
					data={{
						title: comic.metadata.title ?? comic.filesystem.folderName,
						cover
					}}
					events={{
						onClick: () => handleCardClick(comic, cover)
					}}
				>
					{#snippet floatingBadge()}
						{#if bookmarkColor != null}
							<AcerolaBookmarkRibbon
								color={bookmarkColor}
								name={bookmarkName}
								class="-top-1.5 left-5 h-7 w-4"
							/>
						{/if}
					{/snippet}

					{#snippet footer()}
						<div class="mt-1 flex items-center justify-between">
							<span
								class="text-overlay flex items-center gap-1 text-[10px] font-black tracking-wider uppercase"
							>
								<BookOpen size={10} />
								{comic.metadata.chapterCount}
							</span>
						</div>
					{/snippet}

					{#snippet action()}
						<AcerolaButtonIcon
							ui={{
								class: 'text-overlay bg-transparent transition-colors hover:text-primary translate-x-1.5 -mr-1.5'
							}}
							events={{
								onClick: (event) => handleActionClick(event, comic.relations.directoryId)
							}}
						>
							<MoreVertical size={16} />
						</AcerolaButtonIcon>
					{/snippet}

					{#snippet placeholder()}
						<div class="h-full w-full bg-surface">
							<PlaceholderManga class="h-full w-full" />
						</div>
					{/snippet}

					{#snippet overlay()}
						{#if isSelected}
							<div
								class="absolute inset-0 flex items-center justify-center rounded-xl bg-primary/30"
							>
								<div class="rounded-full bg-primary p-2">
									<Check size={24} class="text-primary-foreground" />
								</div>
							</div>
						{:else if selection.isSelectionMode}
							<div
								class="absolute inset-0 flex items-center justify-center rounded-xl bg-surface/50"
							>
								<div class="rounded-full border-2 border-muted-foreground p-2">
									<div class="h-6 w-6"></div>
								</div>
							</div>
						{/if}
					{/snippet}
				</AcerolaCardImage>
			{/each}
		</div>
	</div>

	<AcerolaComicActionDialog
		open={showActionDialog}
		selectedIds={selection.selectedIdsArray}
		totalCount={visibleComics.length}
		bookmarks={bookmarkStore.bookmarks}
		onHide={handleHide}
		onDelete={handleDelete}
		onClearMetadata={handleClearMetadata}
		onBookmark={handleBookmark}
		onSelectAll={handleSelectAllToggle}
		onClose={() => (showActionDialog = false)}
	/>
{:else}
	<div class="flex min-h-[60vh] flex-col items-center justify-center p-8 text-center animate-in fade-in-50 duration-300">
		<div class="mb-4 flex size-16 items-center justify-center rounded-2xl bg-surface/60 text-primary shadow-inner">
			<FolderPlus size={32} />
		</div>
		<h3 class="text-xl font-bold tracking-tight text-foreground">
			{m['pages.home.no_comics']()}
		</h3>
		<p class="mt-1.5 max-w-md text-sm text-muted-foreground">
			{m['pages.home.empty.desc']()}
		</p>
		<div class="mt-6 flex flex-wrap items-center justify-center gap-3">
			<AcerolaButton
				ui={{ variant: 'default', class: 'rounded-xl font-semibold gap-2 shadow-md hover:shadow-lg transition-all' }}
				events={{ onClick: () => refreshScanner.start() }}
			>
				<RefreshCw size={18} class={refreshScanner.scanning ? 'animate-spin' : ''} />
				{m['pages.home.empty.quick_sync']()}
			</AcerolaButton>

			<AcerolaButton
				ui={{ variant: 'outline', class: 'rounded-xl font-medium gap-2' }}
				events={{
					onClick: async () => {
						await folderStore.selectFolder();
						await summary.fetch();
					}
				}}
			>
				<FolderPlus size={16} />
				{m['pages.home.empty.select_folder']()}
			</AcerolaButton>
		</div>
	</div>
{/if}

<!-- Filter Panel (slide-in drawer) -->
<AcerolaFilterPanel
	state={{ open: showFilterPanel }}
	data={{
		sortBy: summary.sortBy,
		sortOrder: summary.sortOrder,
		showHidden: summary.showHidden,
		metadataSource: summary.metadataSource,
		bookmarkFilter,
		bookmarks: bookmarkStore.bookmarks
	}}
	events={{
		onApply: handleFilterApply,
		onClose: () => (showFilterPanel = false)
	}}
/>
