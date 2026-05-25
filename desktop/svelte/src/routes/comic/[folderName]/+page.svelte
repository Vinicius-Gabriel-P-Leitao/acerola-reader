<script lang="ts">
	import AcerolaButtonIcon from '$lib/components/acerola-button/acerola-button-icon.svelte';
	import AcerolaToggleGroup from '$lib/components/acerola-toggle-group/acerola-toggle-group.svelte';
	import { ToggleGroupItem } from '$lib/components/ui/toggle-group/index.js';
	import { STORE_FILE, STORE_KEYS } from '$lib/constants/store-plugin';
	import { useComicChapters } from '$lib/hooks/store/use-comic-chapters.svelte';
	import { useComicContext } from '$lib/state/comic-context.svelte';
	import { resolveArtworkPath, resolveBanner, resolveCover } from '$lib/utils/artwork.utils';
	import ArrowLeft from '@lucide/svelte/icons/arrow-left';
	import { load } from '@tauri-apps/plugin-store';
	import { onMount, untrack } from 'svelte';
	import { fade } from 'svelte/transition';

	import RefreshCw from '@lucide/svelte/icons/refresh-cw';
	import ComicChapterList from './components/comic-chapter-list.svelte';
	import ComicHeroBanner from './components/comic-hero-banner.svelte';
	import ComicMetadataPanel from './components/comic-metadata-panel.svelte';
	import ComicPreferences from './components/comic-preferences.svelte';
	import ComicVolumeList from './components/comic-volume-list.svelte';

	let { data } = $props();

	let expandedVolumeId = $state<string | null>(null);
	let chaptersPerPage = $state(data.initialChaptersPerPage || '25');
	let activeTab = $state('content');
	let displayMode = $state('Lista');
	let volumeViewMode = $state<'cover' | 'banner'>(data.initialVolumeViewMode || 'cover');
	let mediaType = $state('Manga');
	let isAscending = $state(true);
	let searchQuery = $state('');

	const onBack = () => window.history.back();

	const activeComic = useComicContext();
	const chapterStore = useComicChapters();

	onMount(() => {
		if (!activeComic.item && data.comic) {
			activeComic.set(data.comic, resolveCover(data.comic.artwork));
		}
	});

	$effect(() => {
		const vol = expandedVolumeId;
		const comic = activeComic.item ?? data.comic;
		untrack(() => {
			visiblePages = [];

			// Se apenas mudamos de volume no mesmo mangá, mantemos metadados para não dar flicker
			chapterStore.clear(true);
			if (comic) {
				chapterStore.fetch(
					comic.relations.directoryId,
					0,
					parseInt(chaptersPerPage),
					isAscending,
					vol
				);
			}
		});
	});

	$effect(() => {
		const savePreference = async () => {
			const store = await load(STORE_FILE);

			// Somente salva se realmente houve mudança para não causar loops ou acessos de UI desnecessários
			const savedChapters = await store.get<string>(STORE_KEYS.chaptersPerPage);
			const savedVolumeView = await store.get<'cover' | 'banner'>(STORE_KEYS.volumeViewMode);

			let shouldSave = false;

			if (savedChapters !== chaptersPerPage) {
				await store.set(STORE_KEYS.chaptersPerPage, chaptersPerPage);
				shouldSave = true;
			}

			if (savedVolumeView !== volumeViewMode) {
				await store.set(STORE_KEYS.volumeViewMode, volumeViewMode);
				shouldSave = true;
			}

			if (shouldSave) {
				await store.save();
			}
		};
		savePreference();
	});

	let visiblePages = $state<number[]>([]);

	$effect(() => {
		if (chapterStore.loading || visiblePages.length === 0) return;

		const comic = activeComic.item ?? data.comic;
		if (!comic) return;

		for (const page of visiblePages) {
			if (chapterStore.lruKeys.includes(page)) chapterStore.touch(page);
		}

		const missingPages = visiblePages.filter((page) => !chapterStore.lruKeys.includes(page));

		if (missingPages.length > 0) {
			untrack(() => {
				chapterStore.fetch(
					comic.relations.directoryId,
					missingPages[0],

					parseInt(chaptersPerPage),

					isAscending,
					expandedVolumeId
				);
			});
		}
	});

	const manga = $derived.by(() => {
		const item = activeComic.item ?? data.comic;
		if (!item) return null;

		const pageSize = parseInt(chaptersPerPage);
		const chaptersData = chapterStore.chapters;
		const totalItems = chaptersData?.archive.total ?? 0;

		const pagesData = (chaptersData?.pages ?? []).map((it) => ({
			page: it.page,
			items: it.items
				.filter((comic) => {
					// prettier-ignore
					const matchesSearch = comic.name.toLowerCase().includes(searchQuery.toLowerCase());
					// prettier-ignore
					const matchesVolume = !expandedVolumeId || comic.volumeId === expandedVolumeId;

					return matchesSearch && matchesVolume;
				})
				.map((comic) => ({
					id: comic.id.toString(),
					title: comic.name,
					fileName: comic.name,
					isRead: false, // TODO: Isso vai vim do historico
					chapterSort: comic.chapterSort,
					volumeName: comic.volumeName
				}))
		}));

		// Mantemos volumes com info básica, mas chapters reais virão de pagesData filtrado por volume
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
			chaptersCount: totalItems,
			title: item.metadata.title || item.filesystem.folderName,
			cover: resolveCover(item.artwork),
			banner: resolveBanner(item.artwork),
			pagesData,
			volumes,
			pageSize,
			metadata: {
				description: 'Descrição indisponível.',
				author: 'Autor Desconhecido',
				status: 'Desconhecido',
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
			<div class="absolute inset-0 bg-base/40"></div>
		</div>

		<ComicMetadataPanel
			title={manga.title}
			author={manga.metadata.author}
			status={manga.metadata.status}
			source={manga.metadata.source}
			chaptersCount={manga.chaptersCount}
			description={manga.metadata.description}
			cover={manga.cover}
			{onBack}
		/>
		<div
			class="scrollbar-hide relative z-10 flex flex-1 flex-col overflow-y-auto [overflow-anchor:none]"
		>
			<div
				class="sticky top-0 z-50 flex h-20 items-center justify-between border-b border-surface/30 bg-base/90 px-6 backdrop-blur-md lg:hidden"
			>
				<AcerolaButtonIcon onclick={onBack} size="sm">
					<ArrowLeft size={20} />
				</AcerolaButtonIcon>
				<span class="max-w-50 truncate text-sm font-black tracking-widest uppercase">
					{manga.title}
				</span>

				<div class="w-10"></div>
			</div>

			<ComicHeroBanner banner={manga.banner} genres={manga.metadata.genres} />

			<div class="mx-auto w-full max-w-5xl space-y-12 p-8 lg:p-16">
				<div
					class="sticky top-0 z-40 -mx-4 flex items-center justify-between border-b border-surface/30 bg-base/5 px-4 backdrop-blur-3xl"
				>
					<AcerolaToggleGroup type="single" class="flex gap-4" bind:value={activeTab}>
						{#snippet children()}
							<ToggleGroupItem
								value="content"
								class="relative border-none bg-transparent py-6 text-sm font-black tracking-[0.2em] uppercase data-[state=on]:bg-transparent data-[state=on]:text-primary"
							>
								{chapterStore.chapters?.hasVolumeStructure ? 'Volumes' : 'Capítulos'}
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
								Preferências
								{#if activeTab === 'preferences'}<div
										class="absolute right-0 bottom-0 left-0 h-1 rounded-full bg-primary"
										in:fade
									></div>{/if}
							</ToggleGroupItem>
						{/snippet}
					</AcerolaToggleGroup>
					{#if activeTab === 'content' && !chapterStore.chapters?.hasVolumeStructure}
						<div class="flex items-center gap-2 pr-4">
							<input
								type="text"
								bind:value={searchQuery}
								placeholder="FILTRAR..."
								class="w-40 rounded-full border border-surface/30 bg-surface/20 px-6 py-2 text-[10px] font-black tracking-widest transition-all focus:w-60 focus:ring-2 focus:ring-primary/50 focus:outline-none"
							/>
						</div>
					{/if}
				</div>
				<div class="min-h-150">
					{#if activeTab === 'content'}
						{#if chapterStore.chapters?.hasVolumeStructure}
							<ComicVolumeList
								volumes={manga.volumes}
								pagesData={manga.pagesData}
								loading={chapterStore.loading}
								pageSize={manga.pageSize}
								viewMode={volumeViewMode}
								onexpand={(v) => (expandedVolumeId = v)}
								onvisiblepages={(p) => (visiblePages = p)}
							/>
						{:else}
							<ComicChapterList
								pagesData={manga.pagesData}
								totalChapters={manga.chaptersCount}
								pageSize={manga.pageSize}
								onvisiblepages={(p) => (visiblePages = p)}
							/>
						{/if}
					{:else if activeTab === 'preferences'}
						<ComicPreferences
							bind:displayMode
							bind:chaptersPerPage
							bind:mediaType
							bind:volumeViewMode
							hasVolumeStructure={chapterStore.chapters?.hasVolumeStructure ?? false}
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
