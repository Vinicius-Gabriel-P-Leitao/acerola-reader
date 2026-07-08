<script lang="ts">
	import { goto, afterNavigate } from '$app/navigation';
	import AcerolaButton from '$lib/components/acerola-button/acerola-button.svelte';
	import AcerolaButtonIcon from '$lib/components/acerola-button/acerola-button-icon.svelte';
	import AcerolaToggleGroup from '$lib/components/acerola-toggle-group/acerola-toggle-group.svelte';

	import { ToggleGroupItem } from '$lib/components/ui/toggle-group/index.js';

	import { useChaptersPerPage } from '$lib/hooks/preferences/use-chapters-per-page.svelte';
	import { useVolumeViewMode } from '$lib/hooks/preferences/use-volume-view-mode.svelte';
	import { useComicChapters } from '$lib/hooks/store/use-comic-chapters.svelte';
	import { useBookmarks } from '$lib/hooks/store/use-bookmarks.svelte';

	import { useComicContext } from '$lib/state/comic-context.svelte';

	import { resolveArtworkPath, resolveBanner, resolveCover } from '$lib/utils/artwork.utils';
	import type { ReaderChapterPayload } from '$lib/contracts/reader/reader.payloads';
	import { invoke } from '@tauri-apps/api/core';

	import ArrowLeft from '@lucide/svelte/icons/arrow-left';
	import RefreshCw from '@lucide/svelte/icons/refresh-cw';
	import ArrowUpDown from '@lucide/svelte/icons/arrow-up-down';
	import Check from '@lucide/svelte/icons/check';

	import { onMount, untrack } from 'svelte';
	import { fade } from 'svelte/transition';
	import { m } from '$lib/paraglide/messages';

	import ComicChapterList, { type Chapter } from './components/comic-chapter-list.svelte';
	import ComicHeroBanner from './components/comic-hero-banner.svelte';
	import ComicMetadataPanel from './components/comic-metadata-panel.svelte';
	import ComicPreferences from './components/comic-preferences.svelte';
	import ComicVolumeList, { type VolumeChapter } from './components/comic-volume-list.svelte';

	let { data } = $props();

	const activeComic = useComicContext();
	const chapterStore = useComicChapters();

	const chaptersPreference = useChaptersPerPage();
	const volumeViewPreference = useVolumeViewMode();
	const bookmarkStore = useBookmarks();

	let expandedVolumeId = $state<string | null>(null);
	let currentBookmarkId = $state<number | null>(null);

	let activeTab = $state('content');
	let sortBy = $state<'number_asc' | 'number_desc' | 'modified_asc' | 'modified_desc'>('number_asc');
	let searchQuery = $state('');
	let showSortMenu = $state(false);

	let visiblePages = $state<number[]>([]);

	const onBack = () => window.history.back();

	function toReaderChapter(chapter: Chapter | VolumeChapter): ReaderChapterPayload | null {
		if (!chapter.path) return null;

		return {
			id: chapter.id,
			name: chapter.name ?? chapter.title,
			path: chapter.path,
			chapterSort: chapter.chapterSort ?? '',
			volumeId: chapter.volumeId ?? null,
			volumeName: chapter.volumeName ?? null,
			isSpecial: chapter.isSpecial ?? false,
			lastModified: chapter.lastModified ?? 0
		};
	}

	function getReaderProgress(chapter: Chapter | VolumeChapter) {
		const currentManga = manga;
		if (!currentManga) return {};

		const volume = chapter.volumeId
			? currentManga.volumes.find((item) => item.id === chapter.volumeId)
			: null;

		return {
			chapterIndex: chapter.chapterIndex,
			totalChapters: volume?.totalChapters ?? currentManga.chaptersCount,
			chapterScope: volume?.title ?? currentManga.title
		};
	}

	function openReader(chapter: Chapter | VolumeChapter) {
		const readerChapter = toReaderChapter(chapter);
		if (!readerChapter) return;

		goto('/reader', {
			state: {
				chapter: readerChapter,
				comicDirectoryId: activeComic.item?.relations.directoryId ?? data.comic?.relations.directoryId,
				...getReaderProgress(chapter)
			}
		});
	}

	let readingHistory = $state<any | null>(null);
	let readChapters = $state<string[]>([]);
	let isHistoryLoading = $state(false);

	onMount(async () => {
		await Promise.all([
			chaptersPreference.loadChaptersPerPage(),
			volumeViewPreference.loadVolumeViewMode(),
			bookmarkStore.loadBookmarks()
		]);
	});

	afterNavigate(async () => {
		const id = activeComic.item?.relations.directoryId ?? data.comic?.relations.directoryId;
		if (id) {
			isHistoryLoading = true;
			const fetchHistory = invoke('history_get_comic', { comicId: id.toString() }).catch(() => null);
			const fetchRead = invoke<string[]>('history_get_read_chapters', { comicId: id.toString() }).catch(() => []);
			const fetchBookmark = bookmarkStore.getComicBookmark(id);
			
			const [history, read, bookmark] = await Promise.all([fetchHistory, fetchRead, fetchBookmark]);
			readingHistory = history;
			readChapters = read;
			currentBookmarkId = bookmark?.id ?? null;
			
			isHistoryLoading = false;
		}
	});

	function handleReadNow() {
		if (readingHistory) {
			goto('/reader', {
				state: {
					comicDirectoryId: readingHistory.comicDirectoryId.toString(),
					startPage: readingHistory.lastPage,
					chapterScope: readingHistory.comicName,
					chapter: {
						id: readingHistory.chapterArchiveId.toString(),
						name: readingHistory.chapterName,
						path: readingHistory.chapterPath,
						chapterSort: readingHistory.chapterSort,
						isSpecial: readingHistory.isSpecial,
						lastModified: readingHistory.lastModified,
						volumeId: null,
						volumeName: null
					}
				}
			});
		} else {
			// Find first chapter available
			if (manga?.pagesData?.[0]?.items?.[0]) {
				openReader(manga.pagesData[0].items[0] as unknown as Chapter);
			}
		}
	}

	$effect(() => {
		const value = chaptersPreference.chaptersPerPage;

		untrack(() => {
			chaptersPreference.saveChaptersPerPage(value);
		});
	});

	$effect(() => {
		if (data.comic) {
			const comic = data.comic;
			untrack(() => {
				activeComic.set(comic, resolveCover(comic.artwork));
			});
		}
	});

	$effect(() => {
		const value = volumeViewPreference.volumeViewMode;

		untrack(() => {
			volumeViewPreference.saveVolumeViewMode(value);
		});
	});

	$effect(() => {
		const volumeId = expandedVolumeId;
		const query = searchQuery;
		const currentSortBy = sortBy;

		const comic = activeComic.item ?? data.comic;
		if (!comic) return;

		const pageSize = parseInt(chaptersPreference.chaptersPerPage);

		untrack(() => {
			visiblePages = [];
			chapterStore.clear(true);
			chapterStore.fetch(
				comic.relations.directoryId,
				0,
				pageSize,
				currentSortBy,
				volumeId,
				query || null
			);
		});
	});

	$effect(() => {
		if (chapterStore.loading || visiblePages.length === 0) return;

		const comic = activeComic.item ?? data.comic;
		if (!comic) return;

		for (const page of visiblePages) {
			if (chapterStore.lruKeys.includes(page)) {
				chapterStore.touch(page);
			}
		}

		const missingPages = visiblePages.filter((page) => !chapterStore.lruKeys.includes(page));

		if (missingPages.length === 0) return;

		untrack(() => {
			chapterStore.fetch(
				comic.relations.directoryId,
				missingPages[0],
				parseInt(chaptersPreference.chaptersPerPage),
				sortBy,
				expandedVolumeId,
				searchQuery || null
			);
		});
	});

	const manga = $derived.by(() => {
		const item = activeComic.item ?? data.comic;
		if (!item) return null;

		const chaptersData = chapterStore.chapters;
		const totalItems = chaptersData?.archive.total ?? 0;
		const pageSize = parseInt(chaptersPreference.chaptersPerPage);

		const pagesData = (chaptersData?.pages ?? []).map((it) => ({
			page: it.page,

			items: it.items
				.map((comic, index) => ({
					id: comic.id.toString(),
					name: comic.name,
					title: comic.name,
					fileName: comic.name,
					isRead: readChapters.includes(comic.id.toString()),
					chapterSort: comic.chapterSort,
					path: comic.path,
					volumeId: comic.volumeId,
					volumeName: comic.volumeName,
					isSpecial: comic.isSpecial,
					lastModified: comic.lastModified,
					chapterIndex: it.page * pageSize + index
				}))
				.filter((comic) => {
					const matchesVolume = !expandedVolumeId || comic.volumeId === expandedVolumeId;
					return matchesVolume;
				})
		}));

		const volumes = (chaptersData?.archive.volumes ?? []).map((volume) => {
			const volCover = volume.coverUri ? resolveArtworkPath(volume.coverUri) : null;
			const volBanner = volume.bannerUri ? resolveArtworkPath(volume.bannerUri) : null;

			const fallbackCover = resolveCover(item.artwork);
			const fallbackBanner = resolveBanner(item.artwork);

			return {
				id: volume.id.toString(),
				title: volume.name,
				totalChapters: volume.chapterCount,
				coverUri: volCover || fallbackCover,
				bannerUri: volBanner || fallbackBanner || fallbackCover,
				hasMore:
					expandedVolumeId === volume.id.toString() &&
					pagesData.flatMap((page) => page.items).length < volume.chapterCount,
				chapters: []
			};
		});

		return {
			id: item.relations.directoryId.toString(),
			title: item.metadata.title || item.filesystem.folderName,
			chaptersCount: totalItems,
			cover: resolveCover(item.artwork),
			banner: resolveBanner(item.artwork),
			pagesData,
			volumes,
			pageSize,
			metadata: {
				description: m['pages.comic.metadata.description_unavailable'](),
				author: m['pages.comic.metadata.unknown_author'](),
				status: m['pages.comic.metadata.unknown_status'](),
				source: item.metadata.activeSource || 'LOCAL',
				genres: []
			}
		};
	});
</script>

{#if manga}
	<div
		in:fade={{ duration: 200 }}
		out:fade={{ duration: 200 }}
		class="text-text fixed inset-0 z-50 flex h-screen overflow-hidden bg-base"
	>
		<div class="pointer-events-none absolute inset-0 overflow-hidden">
			<img
				alt={manga.title}
				src={manga.banner || manga.cover}
				referrerpolicy="no-referrer"
				class="h-full w-full scale-150 object-cover opacity-20 blur-[120px]"
			/>

			<!-- NOTE: Aqui é onde os dados são injetados no banner -->
			<div class="absolute inset-0 bg-base/40"></div>
		</div>

		<ComicMetadataPanel
			data={{
				title: manga.title,
				author: manga.metadata.author,
				status: manga.metadata.status,
				source: manga.metadata.source,
				chaptersCount: manga.chaptersCount,
				description: manga.metadata.description,
				cover: manga.cover,
				bookmarkColor: bookmarkStore.bookmarks.find(b => b.id === currentBookmarkId)?.color
			}}
			state={{
				isResuming: !!readingHistory,
				isLoading: isHistoryLoading
			}}
			events={{ 
				onBack,
				onReadNow: handleReadNow
			}}
		/>

		<div
			class="scrollbar-hide relative z-10 flex flex-1 flex-col overflow-y-auto [overflow-anchor:none]"
		>
			<div
				class="sticky top-0 z-50 flex h-20 items-center justify-between border-b border-surface/30 bg-base/90 px-6 backdrop-blur-md lg:hidden"
			>
				<AcerolaButtonIcon events={{ onClick: onBack }} ui={{ size: 'sm' }}>
					<ArrowLeft size={20} />
				</AcerolaButtonIcon>

				<span class="max-w-50 truncate text-sm font-black tracking-widest uppercase">
					{manga.title}
				</span>

				<div class="w-10"></div>
			</div>

			<ComicHeroBanner data={{ banner: manga.banner, genres: manga.metadata.genres }} />

			<div class="mx-auto w-full max-w-5xl space-y-12 p-8 lg:p-16">
				<div
					class="sticky top-0 z-40 -mx-4 flex items-center justify-between border-b border-surface/30 bg-base/5 px-4 backdrop-blur-3xl"
				>
					<AcerolaToggleGroup
						config={{ type: 'single' }}
						state={{ value: activeTab }}
						events={{
							onValueChange: (value) => {
								if (typeof value === 'string') activeTab = value;
							}
						}}
						ui={{ class: 'flex gap-4' }}
					>
						{#snippet children()}
							<ToggleGroupItem
								value="content"
								class="relative border-none bg-transparent py-6 text-sm font-black tracking-[0.2em] uppercase data-[state=on]:bg-transparent data-[state=on]:text-primary"
							>
								{chapterStore.chapters?.hasVolumeStructure
									? m['pages.comic.tabs.volumes']()
									: m['pages.comic.tabs.chapters']()}

								{#if activeTab === 'content'}
									<div
										class="absolute right-0 bottom-0 left-0 h-1 rounded-full bg-primary"
										in:fade
									></div>
								{/if}
							</ToggleGroupItem>

							<ToggleGroupItem
								value="preferences"
								class="relative border-none bg-transparent py-6 text-sm font-black tracking-[0.2em] uppercase data-[state=on]:bg-transparent data-[state=on]:text-primary"
							>
								{m['pages.comic.tabs.preferences']()}

								{#if activeTab === 'preferences'}
									<div
										class="absolute right-0 bottom-0 left-0 h-1 rounded-full bg-primary"
										in:fade
									></div>
								{/if}
							</ToggleGroupItem>
						{/snippet}
					</AcerolaToggleGroup>

					{#if activeTab === 'content'}
						<div class="flex items-center gap-2 pr-4">
							{#if !chapterStore.chapters?.hasVolumeStructure}
								<input
									type="text"
									bind:value={searchQuery}
									placeholder={m['pages.comic.filter_placeholder']()}
									class="w-40 rounded-full border border-surface/30 bg-surface/20 px-6 py-2 text-[10px] font-black tracking-widest transition-all focus:w-60 focus:ring-2 focus:ring-primary/50 focus:outline-none"
								/>
							{/if}

							<div class="relative">
								<AcerolaButton
									ui={{ variant: 'ghost', size: 'sm', class: 'rounded-lg' }}
									events={{ onClick: () => (showSortMenu = !showSortMenu) }}
								>
									<ArrowUpDown size={16} />
									{m['pages.comic.sort.button']()}
								</AcerolaButton>

								{#if showSortMenu}
									<div class="absolute right-0 top-full z-50 mt-2 min-w-48 rounded-xl bg-surface shadow-lg">
										<div class="p-2">
											<AcerolaButton
												ui={{ variant: 'ghost', class: 'w-full justify-start rounded-lg' }}
												events={{ onClick: () => { sortBy = 'number_asc'; showSortMenu = false; } }}
											>
												{#if sortBy === 'number_asc'}
													<Check size={16} />
												{:else}
													<div class="w-4"></div>
												{/if}
												{m['pages.comic.sort.number_asc']()}
											</AcerolaButton>
											<AcerolaButton
												ui={{ variant: 'ghost', class: 'w-full justify-start rounded-lg' }}
												events={{ onClick: () => { sortBy = 'number_desc'; showSortMenu = false; } }}
											>
												{#if sortBy === 'number_desc'}
													<Check size={16} />
												{:else}
													<div class="w-4"></div>
												{/if}
												{m['pages.comic.sort.number_desc']()}
											</AcerolaButton>
											<AcerolaButton
												ui={{ variant: 'ghost', class: 'w-full justify-start rounded-lg' }}
												events={{ onClick: () => { sortBy = 'modified_desc'; showSortMenu = false; } }}
											>
												{#if sortBy === 'modified_desc'}
													<Check size={16} />
												{:else}
													<div class="w-4"></div>
												{/if}
												{m['pages.comic.sort.modified_desc']()}
											</AcerolaButton>
											<AcerolaButton
												ui={{ variant: 'ghost', class: 'w-full justify-start rounded-lg' }}
												events={{ onClick: () => { sortBy = 'modified_asc'; showSortMenu = false; } }}
											>
												{#if sortBy === 'modified_asc'}
													<Check size={16} />
												{:else}
													<div class="w-4"></div>
												{/if}
												{m['pages.comic.sort.modified_asc']()}
											</AcerolaButton>
										</div>
									</div>
								{/if}
							</div>
						</div>
					{/if}
				</div>

				<div class="min-h-150">
					{#if activeTab === 'content'}
						{#if chapterStore.chapters?.hasVolumeStructure}
							<ComicVolumeList
								data={{
									volumes: manga.volumes,
									pagesData: manga.pagesData,
									loading: chapterStore.loading,
									pageSize: manga.pageSize,
									viewMode: volumeViewPreference.volumeViewMode
								}}
								events={{
									onExpand: (value) => (expandedVolumeId = value),
									onVisiblePages: (pages) => (visiblePages = pages),
									onOpenChapter: openReader
								}}
							/>
						{:else}
							<ComicChapterList
								data={{
									pagesData: manga.pagesData,
									totalChapters: manga.chaptersCount,
									pageSize: manga.pageSize
								}}
								events={{
									onVisiblePages: (pages: number[]) => (visiblePages = pages),
									onOpenChapter: openReader
								}}
							/>
						{/if}
					{:else if activeTab === 'preferences'}
						<ComicPreferences
							data={{
								hasVolumeStructure: chapterStore.chapters?.hasVolumeStructure ?? false,
								bookmarks: bookmarkStore.bookmarks
							}}
							state={{
								chaptersPerPage: chaptersPreference.chaptersPerPage,
								volumeViewMode: volumeViewPreference.volumeViewMode,
								bookmarkId: currentBookmarkId
							}}
							events={{
								onChaptersPerPageChange: (value) => (chaptersPreference.chaptersPerPage = value),
								onVolumeViewModeChange: (value) => (volumeViewPreference.volumeViewMode = value),
								onBookmarkChange: async (value) => {
									const id = activeComic.item?.relations.directoryId ?? data.comic?.relations.directoryId;
									if (!id) return;
									
									if (value) {
										await bookmarkStore.assignToComic(id, value);
									} else {
										await bookmarkStore.removeComicBookmark(id);
									}
									currentBookmarkId = value;
								}
							}}
						/>
					{/if}
				</div>
			</div>
		</div>
	</div>
{:else}
	<div class="text-overlay flex h-screen items-center justify-center bg-base">
		<RefreshCw size={48} class="animate-spin text-primary" />
	</div>
{/if}
