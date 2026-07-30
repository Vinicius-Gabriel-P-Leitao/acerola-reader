<script lang="ts">
	import { goto } from '$app/navigation';
	import PlaceholderManga from '$lib/assets/placeholder/placeholder_manga.svg?component';
	import AcerolaButton from '$lib/components/acerola-button/acerola-button.svelte';
	import AcerolaButtonIcon from '$lib/components/acerola-button/acerola-button-icon.svelte';
	import AcerolaCardImage from '$lib/components/acerola-card/acerola-card-image.svelte';
	import AcerolaBookmarkRibbon from '$lib/components/acerola-bookmark-ribbon/acerola-bookmark-ribbon.svelte';
	import AcerolaComicActionDialog from './components/acerola-comic-action-dialog.svelte';
	import { useBookmarks } from '$lib/hooks/store/use-bookmarks.svelte';
	import { useComicSelection } from '$lib/hooks/store/use-comic-selection.svelte';
	import MoreHorizontal from '@lucide/svelte/icons/more-horizontal';
	import BookOpen from '@lucide/svelte/icons/book-open';
	import Check from '@lucide/svelte/icons/check';
	import ArrowUpDown from '@lucide/svelte/icons/arrow-up-down';
	import SlidersHorizontal from '@lucide/svelte/icons/sliders-horizontal';
	import { LIBRARY_EVENTS } from '$lib/contracts/library/library.events';
	import { useComicSummary } from '$lib/hooks/store/use-comic-summary.svelte';
	import { useComicContext } from '$lib/state/comic-context.svelte';
	import { resolveCover } from '$lib/utils/artwork.utils';
	import { listen } from '@tauri-apps/api/event';
	import { onDestroy, onMount } from 'svelte';
	import { m } from '$lib/paraglide/messages';
	import { toast } from 'svelte-sonner';
	import type { ComicSummaryItemPayload } from '$lib/contracts/home/home.payloads';

	const summary = useComicSummary();
	const activeComic = useComicContext();
	const bookmarkStore = useBookmarks();
	const selection = useComicSelection();

	let unlistenScan: (() => void) | undefined;
	let showSortMenu = $state(false);
	let showActionDialog = $state(false);

	onMount(async () => {
		await bookmarkStore.loadBookmarks(true);
		unlistenScan = await listen(LIBRARY_EVENTS.scanComplete, async () => {
			await summary.fetch();
			await bookmarkStore.loadBookmarks(true);
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
		}
		if (failCount > 0) {
			toast.error(m['pages.home.toast.bookmark_error']());
			await summary.fetch();
			await bookmarkStore.loadBookmarks(true);
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
		if (!selection.isSelected(comicId)) {
			selection.toggleSelection(comicId);
		}
		showActionDialog = true;
	}

	function handleSelectAllToggle() {
		const allIds = summary.comics?.comics.map((c) => c.relations.directoryId) ?? [];
		if (selection.selectedCount === allIds.length && allIds.length > 0) {
			selection.deselectAll();
		} else {
			selection.selectAll(allIds);
		}
	}

	function handleSort(sortBy: 'title' | 'chapterCount', sortOrder: 'asc' | 'desc') {
		summary.setSorting(sortBy, sortOrder);
		summary.fetch();
		showSortMenu = false;
	}
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
						{selection.selectedCount === (summary.comics?.comics.length ?? 0)
							? m['pages.home.selection.deselect_all']()
							: m['pages.home.selection.select_all']()}
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
				<div class="relative">
					<AcerolaButton
						ui={{ variant: 'ghost', class: 'rounded-xl' }}
						events={{ onClick: () => (showSortMenu = !showSortMenu) }}
					>
						<ArrowUpDown size={16} />
						{m['pages.home.sort.button']()}
					</AcerolaButton>

					{#if showSortMenu}
						<div
							class="absolute top-full left-0 z-50 mt-2 min-w-48 rounded-xl bg-surface shadow-lg"
						>
							<div class="p-2">
								<AcerolaButton
									ui={{ variant: 'ghost', class: 'w-full justify-start rounded-lg' }}
									events={{ onClick: () => handleSort('title', 'asc') }}
								>
									{#if summary.sortBy === 'title' && summary.sortOrder === 'asc'}
										<Check size={16} />
									{:else}
										<div class="w-4"></div>
									{/if}
									{m['pages.home.sort.title_asc']()}
								</AcerolaButton>
								<AcerolaButton
									ui={{ variant: 'ghost', class: 'w-full justify-start rounded-lg' }}
									events={{ onClick: () => handleSort('title', 'desc') }}
								>
									{#if summary.sortBy === 'title' && summary.sortOrder === 'desc'}
										<Check size={16} />
									{:else}
										<div class="w-4"></div>
									{/if}
									{m['pages.home.sort.title_desc']()}
								</AcerolaButton>
								<AcerolaButton
									ui={{ variant: 'ghost', class: 'w-full justify-start rounded-lg' }}
									events={{ onClick: () => handleSort('chapterCount', 'asc') }}
								>
									{#if summary.sortBy === 'chapterCount' && summary.sortOrder === 'asc'}
										<Check size={16} />
									{:else}
										<div class="w-4"></div>
									{/if}
									{m['pages.home.sort.chapter_asc']()}
								</AcerolaButton>
								<AcerolaButton
									ui={{ variant: 'ghost', class: 'w-full justify-start rounded-lg' }}
									events={{ onClick: () => handleSort('chapterCount', 'desc') }}
								>
									{#if summary.sortBy === 'chapterCount' && summary.sortOrder === 'desc'}
										<Check size={16} />
									{:else}
										<div class="w-4"></div>
									{/if}
									{m['pages.home.sort.chapter_desc']()}
								</AcerolaButton>
							</div>
						</div>
					{/if}
				</div>
			{/if}
		</div>

		<div class="grid grid-cols-[repeat(auto-fill,minmax(9rem,1fr))] gap-6">
			{#each summary.comics.comics as comic (comic.relations.directoryId)}
				{@const cover = resolveCover(comic.artwork)}
				{@const bookmarkColor = bookmarkStore.getBookmarkForComic(
					comic.relations.directoryId
				)?.color}
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
							<AcerolaBookmarkRibbon color={bookmarkColor} class="-top-1.5 left-5 h-10 w-6" />
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
								class: 'text-overlay bg-transparent transition-colors hover:text-primary'
							}}
							events={{
								onClick: (e) => handleActionClick(e, comic.relations.directoryId)
							}}
						>
							<MoreHorizontal size={16} />
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
		totalCount={summary.comics?.comics.length ?? 0}
		bookmarks={bookmarkStore.bookmarks}
		onHide={handleHide}
		onDelete={handleDelete}
		onBookmark={handleBookmark}
		onSelectAll={handleSelectAllToggle}
		onClose={() => (showActionDialog = false)}
	/>
{:else}
	<div class="flex items-center justify-center p-8 text-muted-foreground">
		{m['pages.home.no_comics']()}
	</div>
{/if}
