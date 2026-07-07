<script lang="ts">
	import { goto } from '$app/navigation';
	import PlaceholderManga from '$lib/assets/placeholder/placeholder_manga.svg?component';
	import AcerolaButtonIcon from '$lib/components/acerola-button/acerola-button-icon.svelte';
	import AcerolaCardImage from '$lib/components/acerola-card/acerola-card-image.svelte';
	import AcerolaBookmarkRibbon from '$lib/components/acerola-bookmark-ribbon/acerola-bookmark-ribbon.svelte';
	import { useBookmarks } from '$lib/hooks/store/use-bookmarks.svelte';
	import MoreHorizontal from '@lucide/svelte/icons/more-horizontal';
	import BookOpen from '@lucide/svelte/icons/book-open';

	import { LIBRARY_EVENTS } from '$lib/contracts/library/library.events';
	import { useComicSummary } from '$lib/hooks/store/use-comic-summary.svelte';
	import { useComicContext } from '$lib/state/comic-context.svelte';
	import { globalSearch } from '$lib/state/search.svelte';
	import { resolveCover } from '$lib/utils/artwork.utils';
	import { listen } from '@tauri-apps/api/event';
	import { onDestroy, onMount } from 'svelte';
	import { m } from '$lib/paraglide/messages';

	const summary = useComicSummary();
	const activeComic = useComicContext();
	const bookmarkStore = useBookmarks();

	let unlistenScan: (() => void) | undefined;
	let searchTimeout: ReturnType<typeof setTimeout> | undefined = undefined;

	onMount(async () => {
		unlistenScan = await listen(LIBRARY_EVENTS.scanComplete, async () => {
			await summary.fetch(globalSearch.query);
		});

		await summary.fetch(globalSearch.query);
	});

	onDestroy(() => {
		unlistenScan?.();
	});

	$effect(() => {
		const query = globalSearch.query;
		if (searchTimeout) clearTimeout(searchTimeout);
		searchTimeout = setTimeout(() => {
			summary.fetch(query);
		}, 300);
	});
</script>

{#if summary.loading && (!summary.comics || summary.comics.total === 0)}
	<div class="flex items-center justify-center p-8 text-muted-foreground">
		{m['pages.home.loading']()}
	</div>
{:else if summary.comics && summary.comics.total > 0}
	<div class="px-8 pb-8 pt-8">
		<div class="grid grid-cols-[repeat(auto-fill,minmax(9rem,1fr))] gap-6">
			{#each summary.comics.comics as comic (comic.relations.directoryId)}
				{@const cover = resolveCover(comic.artwork)}
				{@const bookmarkColor = bookmarkStore.getBookmarkForComic(comic.relations.directoryId)?.color}
				<AcerolaCardImage
					data={{
						title: comic.metadata.title ?? comic.filesystem.folderName,
						cover
					}}
					events={{
						onClick: () => {
							activeComic.set(comic, cover);
							goto(`/comic/${comic.filesystem.folderName}`);
						}
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
						>
							<MoreHorizontal size={16} />
						</AcerolaButtonIcon>
					{/snippet}

					{#snippet placeholder()}
						<div class="h-full w-full bg-surface">
							<PlaceholderManga class="h-full w-full" />
						</div>
					{/snippet}
				</AcerolaCardImage>
			{/each}
		</div>
	</div>
{:else}
	<div class="flex items-center justify-center p-8 text-muted-foreground">
		{m['pages.home.no_comics']()}
	</div>
{/if}
