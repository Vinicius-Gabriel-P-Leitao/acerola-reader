<script lang="ts">
	import { goto } from '$app/navigation';
	import PlaceholderManga from '$lib/assets/placeholder/placeholder_manga.svg?component';
	import AcerolaButton from '$lib/components/acerola-button/acerola-button.svelte';
	import AcerolaButtonIcon from '$lib/components/acerola-button/acerola-button-icon.svelte';
	import AcerolaCardImage from '$lib/components/acerola-card/acerola-card-image.svelte';
	import AcerolaBookmarkRibbon from '$lib/components/acerola-bookmark-ribbon/acerola-bookmark-ribbon.svelte';
	import AcerolaComicActionDialog from '$lib/components/acerola-comic-action-dialog/acerola-comic-action-dialog.svelte';
	import { useBookmarks } from '$lib/hooks/store/use-bookmarks.svelte';
	import { useComicSelection } from '$lib/hooks/store/use-comic-selection.svelte';
	import MoreHorizontal from '@lucide/svelte/icons/more-horizontal';
	import BookOpen from '@lucide/svelte/icons/book-open';
	import Check from '@lucide/svelte/icons/check';
	import ArrowUpDown from '@lucide/svelte/icons/arrow-up-down';
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

	onMount(async () => {
		await bookmarkStore.loadBookmarks();
		unlistenScan = await listen(LIBRARY_EVENTS.scanComplete, async () => {
			await summary.fetch();
		});

		await summary.fetch();
	});

	onDestroy(() => {
		unlistenScan?.();
	});

	async function handleHide(ids: number[]) {
		const count = await summary.updateVisibility(ids, true);
		toast.success(m['pages.home.toast.hidden']({ count }));
	}

	async function handleDelete(ids: number[]) {
		const count = await summary.deleteComics(ids);
		toast.success(m['pages.home.toast.deleted']({ count }));
	}

	async function handleBookmark(ids: number[], categoryId: number) {
		for (const id of ids) {
			await bookmarkStore.assignToComic(id, categoryId);
		}
		toast.success(m['pages.home.toast.bookmarked']({ count: ids.length }));
	}

	function handleCardClick(comic: ComicSummaryItemPayload, cover: string | null) {
		if (selection.isSelectionMode) {
			selection.toggleSelection(Number(comic.relations.directoryId));
		} else {
			activeComic.set(comic, cover);
			goto(`/comic/${comic.filesystem.folderName}`);
		}
	}

	function handleActionClick(event: MouseEvent, comicId: number) {
		event.stopPropagation();
		selection.selectSingle(comicId);
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
					<span class="text-sm text-muted-foreground">
						{m['pages.home.selection.selected']({ count: selection.selectedCount })}
					</span>
					<AcerolaButton
						ui={{ variant: 'ghost', size: 'sm', class: 'rounded-lg' }}
						events={{
							onClick: () =>
								selection.selectAll(
									summary.comics?.comics.map((c) => Number(c.relations.directoryId)) ?? []
								)
						}}
					>
						{m['pages.home.selection.select_all']()}
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
				{@const isSelected = selection.isSelected(Number(comic.relations.directoryId))}
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
							<AcerolaBookmarkRibbon color={bookmarkColor} />
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
								onClick: (e) => handleActionClick(e, Number(comic.relations.directoryId))
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
		selectedIds={selection.selectedIdsArray}
		bookmarks={bookmarkStore.bookmarks}
		onHide={handleHide}
		onDelete={handleDelete}
		onBookmark={handleBookmark}
		onClose={() => selection.exitSelectionMode()}
	/>
{:else}
	<div class="flex items-center justify-center p-8 text-muted-foreground">
		{m['pages.home.no_comics']()}
	</div>
{/if}
