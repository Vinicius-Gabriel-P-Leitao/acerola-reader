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
			isSelectionMode?: boolean;
			isSelected?: (id: string) => boolean;
		};
		events?: {
			onVisiblePages?: (pages: number[]) => void;
			onOpenChapter?: (chapter: Chapter) => void;
			onToggleSelect?: (chapter: Chapter) => void;
			onEnterSelection?: (chapter: Chapter) => void;
			onMarkRead?: (chapter: Chapter) => void;
			onMarkUnread?: (chapter: Chapter) => void;
		};
	};
</script>

<script lang="ts">
	import { onMount } from 'svelte';
	import AcerolaHeroButton from '$lib/components/acerola-hero-button/acerola-hero-button.svelte';
	import AcerolaButton from '$lib/components/acerola-button/acerola-button.svelte';
	import AcerolaPopover from '$lib/components/acerola-popover/acerola-popover.svelte';
	import BookOpen from '@lucide/svelte/icons/book-open';
	import Check from '@lucide/svelte/icons/check';
	import CheckSquare from '@lucide/svelte/icons/check-square';
	import MoreVertical from '@lucide/svelte/icons/more-vertical';
	import RefreshCw from '@lucide/svelte/icons/refresh-cw';
	import { m } from '$lib/paraglide/messages';

	let { data, events }: ComicChapterListProps = $props();

	let openMenuId = $state<string | null>(null);

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
									onClick: () =>
										data.isSelectionMode
											? events?.onToggleSelect?.(chapter)
											: events?.onOpenChapter?.(chapter)
								}}
								ui={{
									class: chapter.isRead
										? 'h-full flex-nowrap overflow-hidden border-primary/30 bg-primary/10 hover:bg-primary/20'
										: 'h-full flex-nowrap overflow-hidden border-surface/40 bg-mantle/40 hover:bg-surface/30'
								}}
							>
								{#snippet icon()}
									{#if data.isSelectionMode}
										{#if data.isSelected?.(chapter.id)}
											<div
												class="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-crust"
											>
												<Check size={16} strokeWidth={3} />
											</div>
										{:else}
											<div class="flex h-6 w-6 items-center justify-center">
												<div class="h-5 w-5 rounded-full border-2 border-muted-foreground"></div>
											</div>
										{/if}
									{:else}
										<div class={chapter.isRead ? '' : 'text-primary'}>
											{#if chapter.isRead}
												<div
													class="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-crust"
												>
													<Check size={16} strokeWidth={3} />
												</div>
											{:else}
												<BookOpen size={24} />
											{/if}
										</div>
									{/if}
								{/snippet}

								{#snippet action()}
									<div class="flex items-center gap-2">
										{#if chapter.isRead}
											<span
												class="rounded-full bg-primary/10 px-3 py-1 text-[10px] font-black tracking-widest text-primary uppercase"
											>
												{m['pages.comic.metadata.completed']
													? m['pages.comic.metadata.completed']()
													: 'LIDO'}
											</span>
										{/if}
										<div role="presentation" onclick={(event) => event.stopPropagation()}>
											<AcerolaPopover
												state={{ open: openMenuId === chapter.id }}
												events={{
													onOpenChange: (open) => (openMenuId = open ? chapter.id : null)
												}}
												ui={{
													align: 'end',
													contentClass:
														'w-52 overflow-hidden rounded-2xl border-border/40 bg-card/95 p-1.5 shadow-2xl backdrop-blur-md'
												}}
											>
												{#snippet trigger()}
													<span
														class="flex size-10 items-center justify-center rounded-xl text-overlay transition-colors hover:bg-surface/60 hover:text-primary"
													>
														<MoreVertical size={20} />
													</span>
												{/snippet}

												{#snippet content()}
													<div class="flex flex-col gap-0.5">
														{#if chapter.isRead}
															<AcerolaButton
																ui={{
																	variant: 'ghost',
																	class:
																		'h-9 w-full justify-start gap-2.5 rounded-xl px-2.5 text-sm font-medium text-amber-500 hover:bg-amber-500/10 hover:text-amber-400'
																}}
																events={{
																	onClick: () => {
																		openMenuId = null;
																		events?.onMarkUnread?.(chapter);
																	}
																}}
															>
																<BookOpen size={16} class="shrink-0" />
																{m['pages.comic.selection.mark_unread']()}
															</AcerolaButton>
														{:else}
															<AcerolaButton
																ui={{
																	variant: 'ghost',
																	class:
																		'h-9 w-full justify-start gap-2.5 rounded-xl px-2.5 text-sm font-medium text-primary hover:bg-primary/10'
																}}
																events={{
																	onClick: () => {
																		openMenuId = null;
																		events?.onMarkRead?.(chapter);
																	}
																}}
															>
																<Check size={16} class="shrink-0" />
																{m['pages.comic.selection.mark_read']()}
															</AcerolaButton>
														{/if}

														<div class="mx-1 my-1 h-px bg-border/60"></div>

														<AcerolaButton
															ui={{
																variant: 'ghost',
																class:
																	'h-9 w-full justify-start gap-2.5 rounded-xl px-2.5 text-sm font-medium'
															}}
															events={{
																onClick: () => {
																	openMenuId = null;
																	events?.onEnterSelection?.(chapter);
																}
															}}
														>
															<CheckSquare size={16} class="shrink-0 text-muted-foreground" />
															{m['pages.comic.selection.select']()}
														</AcerolaButton>
													</div>
												{/snippet}
											</AcerolaPopover>
										</div>
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
