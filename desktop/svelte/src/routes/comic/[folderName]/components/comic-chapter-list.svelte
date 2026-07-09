<script lang="ts" module>
	export type Chapter = {
		id: string;
		name?: string;
		title: string;
		fileName: string;
		isRead: boolean;
		chapterSort: string;
		path?: string;
		volumeId?: string | null;
		volumeName?: string | null;
		isSpecial?: boolean;
		lastModified?: number;
		chapterIndex?: number;
	};

	export type ChapterPage = {
		page: number;
		items: Chapter[];
	};

	export type ComicChapterListProps = {
		data: {
			pagesData: ChapterPage[];
			totalChapters: number;
			pageSize: number;
		};
		events?: {
			onVisiblePages?: (pages: number[]) => void;
			onOpenChapter?: (chapter: Chapter) => void;
		};
	};
</script>

<script lang="ts">
	import { onMount } from 'svelte';
	import AcerolaHeroButton from '$lib/components/acerola-hero-button/acerola-hero-button.svelte';
	import AcerolaButtonIcon from '$lib/components/acerola-button/acerola-button-icon.svelte';
	import BookOpen from '@lucide/svelte/icons/book-open';
	import Check from '@lucide/svelte/icons/check';
	import MoreVertical from '@lucide/svelte/icons/more-vertical';
	import RefreshCw from '@lucide/svelte/icons/refresh-cw';
	import { m } from '$lib/paraglide/messages';

	let { data, events }: ComicChapterListProps = $props();

	/**
	 * ITEM_HEIGHT (112px): Altura total do slot (item + gap).
	 * BUTTON_HEIGHT (100px): Altura real do botão para caber Title + Description + Padding.
	 */
	const ITEM_HEIGHT = 112;
	const BUTTON_HEIGHT = 100;

	const totalPages = $derived(Math.ceil(data.totalChapters / data.pageSize));

	let visiblePages = new Set<number>();
	let observer: IntersectionObserver | null = null;

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

				if (changed && events?.onVisiblePages) {
					events.onVisiblePages(Array.from(visiblePages).sort((a, b) => a - b));
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

<div class="flex w-full flex-col">
	{#if totalPages > 0}
		{#each Array(totalPages) as _, pageIndex}
			{@const blockData = data.pagesData.find((page) => page.page === pageIndex)}
			<!-- prettier-ignore -->
			{@const itemsInThisPage = pageIndex === totalPages - 1 ? data.totalChapters % data.pageSize || data.pageSize : data.pageSize}

			<!-- 
        PAGE BLOCK (Relative): 
        Ocupa o espaço exato da página no fluxo do documento.
        Items dentro são Absolute para garantir estabilidade visual total.
      -->
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
								data={{
									title: chapter.title,
									description: chapter.fileName
								}}
								events={{
									onClick: () => events?.onOpenChapter?.(chapter)
								}}
								ui={{
									class: chapter.isRead
										? 'h-full flex-nowrap overflow-hidden border-primary/30 bg-primary/10 hover:bg-primary/20'
										: 'h-full flex-nowrap overflow-hidden border-surface/40 bg-mantle/40 hover:bg-surface/30'
								}}
							>
								{#snippet icon()}
									<div class={chapter.isRead ? '' : 'text-primary'}>
										{#if chapter.isRead}
											<div class="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-crust">
												<Check size={16} strokeWidth={3} />
											</div>
										{:else}
											<BookOpen size={24} />
										{/if}
									</div>
								{/snippet}

								{#snippet action()}
									<div class="flex items-center gap-2">
										{#if chapter.isRead}
											<span class="rounded-full bg-primary/10 px-3 py-1 text-[10px] font-black tracking-widest text-primary uppercase">
												{m['pages.comic.metadata.completed'] ? m['pages.comic.metadata.completed']() : 'LIDO'}
											</span>
										{/if}
										<AcerolaButtonIcon
											events={{
												onClick: (event) => event.stopPropagation()
											}}
											ui={{
												variant: 'ghost',
												size: 'sm',
												class: 'text-overlay hover:text-primary'
											}}
										>
											<MoreVertical size={20} />
										</AcerolaButtonIcon>
									</div>
								{/snippet}
							</AcerolaHeroButton>
						</div>
					{/each}
				{/if}
			</div>
		{/each}
	{:else}
		<div class="space-y-4 py-20 text-center opacity-50">
			<RefreshCw size={48} class="mx-auto animate-spin text-primary" />
			<p class="text-sm font-black tracking-widest uppercase">{m['pages.comic.loading']()}</p>
		</div>
	{/if}
</div>
