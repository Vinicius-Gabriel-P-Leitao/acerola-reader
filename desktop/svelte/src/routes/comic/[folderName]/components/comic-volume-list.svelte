<script lang="ts" module>
	export type VolumeChapter = {
		id: string;
		name?: string;
		title: string;
		fileName: string;
		isRead: boolean;
		chapterSort?: string;
		path?: string;
		volumeId?: string | null;
		volumeName?: string | null;
		isSpecial?: boolean;
		lastModified?: number;
		chapterIndex?: number;
	};

	export type Volume = {
		id: string;
		title: string;
		chapters: VolumeChapter[];
		totalChapters: number;
		hasMore: boolean;
		coverUri?: string | null;
		bannerUri?: string | null;
	};

	export type VolumePage = {
		page: number;
		items: VolumeChapter[];
	};
</script>

<script lang="ts">
	import { cn } from '$lib/utils/cn.utils';
	import { slide } from 'svelte/transition';
	import AcerolaHeroButton from '$lib/components/acerola-hero-button/acerola-hero-button.svelte';
	import AcerolaButtonIcon from '$lib/components/acerola-button/acerola-button-icon.svelte';
	import ComicVolumeButton from './comic-volume-button.svelte';
	import Folder from '@lucide/svelte/icons/folder';
	import ChevronDown from '@lucide/svelte/icons/chevron-down';
	import BookOpen from '@lucide/svelte/icons/book-open';
	import MoreVertical from '@lucide/svelte/icons/more-vertical';
	import RefreshCw from '@lucide/svelte/icons/refresh-cw';
	import { onMount } from 'svelte';
	import { m } from '$lib/paraglide/messages';

	let {
		volumes = [],
		pagesData = [],
		loading = false,
		pageSize = 25,
		viewMode = 'cover',
		onexpand,
		onvisiblepages,
		onopenchapter
	}: {
		volumes: Volume[];
		pagesData?: VolumePage[];
		loading?: boolean;
		pageSize?: number;
		viewMode?: 'cover' | 'banner';
		onexpand: (volumeId: string | null) => void;
		onvisiblepages?: (pages: number[]) => void;
		onopenchapter?: (chapter: VolumeChapter) => void;
	} = $props();

	let expandedVolumeId = $state<string | null>(null);
	let visiblePages = new Set<number>();
	let observer: IntersectionObserver | null = null;

	const ITEM_HEIGHT = 112;
	const BUTTON_HEIGHT = 100;

	const toggleVolume = (volumeId: string) => {
		if (expandedVolumeId === volumeId) {
			expandedVolumeId = null;
			visiblePages.clear();

			if (onvisiblepages) onvisiblepages([]);
		} else {
			expandedVolumeId = volumeId;
		}

		onexpand(expandedVolumeId);
	};

	onMount(() => {
		observer = new IntersectionObserver(
			(entries) => {
				let changed = false;

				entries.forEach((entry) => {
					const pageStr = (entry.target as HTMLElement).dataset.page;
					if (!pageStr) return;

					const page = parseInt(pageStr, 10);
					const wasVisible = visiblePages.has(page);

					if (entry.isIntersecting) {
						visiblePages.add(page);
					} else {
						visiblePages.delete(page);
					}

					if (wasVisible !== entry.isIntersecting) {
						changed = true;
					}
				});

				if (changed && onvisiblepages) {
					onvisiblepages(Array.from(visiblePages).sort((a, b) => a - b));
				}
			},
			{ rootMargin: '1200px 0px' }
		);
		return () => observer?.disconnect();
	});

	function trackPage(node: HTMLElement, page: number) {
		node.dataset.page = page.toString();
		observer?.observe(node);
		return {
			destroy() {
				observer?.unobserve(node);
			}
		};
	}
</script>

<div class="space-y-4">
	{#if volumes && volumes.length > 0}
		{#each volumes as volume}
			<ComicVolumeButton
				title={volume.title}
				totalChapters={volume.totalChapters}
				{viewMode}
				coverUri={volume.coverUri}
				bannerUri={volume.bannerUri}
				isExpanded={expandedVolumeId === volume.id}
				onclick={() => toggleVolume(volume.id)}
			/>

			{#if expandedVolumeId === volume.id}
				{@const totalPages = Math.ceil(volume.totalChapters / pageSize)}

				<div transition:slide class="ml-10 border-l border-surface/30 p-4 pt-0">
					<div class="flex w-full flex-col">
						{#if volume.totalChapters > 0}
							{#each Array(totalPages) as _, pageIndex}
								{@const blockData = pagesData.find((page) => page.page === pageIndex)}
								<!-- prettier-ignore -->
								{@const itemsInThisPage = pageIndex === totalPages - 1? volume.totalChapters % pageSize || pageSize: pageSize}

								<div
									use:trackPage={pageIndex}
									class="relative w-full"
									style:height="{itemsInThisPage * ITEM_HEIGHT}px"
								>
									{#if blockData && blockData.items.length > 0}
										{#each blockData.items as chapter, index (chapter.id)}
											<div
												class="absolute right-0 left-0 animate-in duration-300 fade-in"
												style:top="{index * ITEM_HEIGHT}px"
												style:height="{BUTTON_HEIGHT}px"
											>
												<AcerolaHeroButton
													title={chapter.title}
													description={chapter.fileName}
													onclick={() => onopenchapter?.(chapter)}
													class="h-full flex-nowrap overflow-hidden border-surface/40 bg-mantle/40 hover:bg-surface/30"
												>
													{#snippet icon()}
														<div class={chapter.isRead ? 'text-overlay' : 'text-primary'}>
															<BookOpen size={24} />
														</div>
													{/snippet}

													{#snippet action()}
														<AcerolaButtonIcon
															variant="ghost"
															size="sm"
															onclick={(event) => event.stopPropagation()}
															class="text-overlay hover:text-primary"
														>
															<MoreVertical size={20} />
														</AcerolaButtonIcon>
													{/snippet}
												</AcerolaHeroButton>
											</div>
										{/each}
									{/if}
								</div>
							{/each}
						{/if}

						{#if loading}
							<div class="flex items-center justify-center py-4">
								<RefreshCw size={24} class="animate-spin text-primary" />
							</div>
						{/if}

						{#if volume.totalChapters === 0 && !loading}
							<p class="py-4 text-center text-xs font-black tracking-widest uppercase opacity-50">
								{m['pages.comic.volume.no_chapters']()}
							</p>
						{/if}
					</div>
				</div>
			{/if}
		{/each}
	{:else}
		<div
			class="space-y-4 rounded-3xl border-2 border-dashed border-surface py-20 text-center opacity-50"
		>
			<RefreshCw size={48} class="animate-spin text-primary" />
			<p class="text-sm font-black tracking-widest uppercase">
				{m['pages.comic.volume.no_volumes']()}
			</p>
		</div>
	{/if}
</div>
