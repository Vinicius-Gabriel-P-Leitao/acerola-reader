<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import AcerolaButtonIcon from '$lib/components/acerola-button/acerola-button-icon.svelte';
	import { useReader } from '$lib/hooks/store/use-reader.svelte';
	import type { ReaderChapterPayload } from '$lib/contracts/reader/reader.payloads';
	import ArrowLeft from '@lucide/svelte/icons/arrow-left';
	import ChevronLeft from '@lucide/svelte/icons/chevron-left';
	import ChevronRight from '@lucide/svelte/icons/chevron-right';
	import RefreshCw from '@lucide/svelte/icons/refresh-cw';
	import { onMount, untrack } from 'svelte';
	import { fade } from 'svelte/transition';

	type ReaderNavigationState = {
		chapter?: ReaderChapterPayload;
		startPage?: number;
	};

	const reader = useReader();

	let observer: IntersectionObserver | null = null;
	let visibleSet = new Set<number>();
	let visiblePages = $state<number[]>([]);
	let openFailed = $state(false);

	const navigationState = $derived((page.state ?? {}) as ReaderNavigationState);
	const chapter = $derived(navigationState.chapter);

	function leaveReader() {
		if (window.history.length > 1) {
			window.history.back();
			return;
		}

		goto('/home');
	}

	onMount(() => {
		observer = new IntersectionObserver(
			(entries) => {
				let changed = false;

				for (const entry of entries) {
					const pageIndex = Number((entry.target as HTMLElement).dataset.page);
					if (!Number.isFinite(pageIndex)) continue;

					const wasVisible = visibleSet.has(pageIndex);

					if (entry.isIntersecting) {
						visibleSet.add(pageIndex);
					} else {
						visibleSet.delete(pageIndex);
					}

					if (wasVisible !== entry.isIntersecting) {
						changed = true;
					}
				}

				if (changed) {
					visiblePages = Array.from(visibleSet).sort((left, right) => left - right);
				}
			},
			{ rootMargin: '40% 0px' }
		);

		const start = async () => {
			if (!chapter) return;

			try {
				await reader.open(chapter, navigationState.startPage ?? 0);
			} catch {
				openFailed = true;
			}
		};

		void start();

		return () => observer?.disconnect();
	});

	$effect(() => {
		if (!reader.session || visiblePages.length === 0) return;

		const targetPage = visiblePages[0];

		untrack(() => {
			if (targetPage !== reader.currentPage) {
				void reader.goToPage(targetPage);
			}

			for (const pageIndex of visiblePages) {
				void reader.loadPage(pageIndex, false).catch(() => undefined);
			}
		});
	});

	function trackPage(node: HTMLElement, pageIndex: number) {
		node.dataset.page = pageIndex.toString();
		observer?.observe(node);

		return {
			destroy() {
				observer?.unobserve(node);
			}
		};
	}
</script>

<div class="fixed inset-0 z-50 flex h-screen flex-col overflow-hidden bg-base text-text">
	<header
		class="relative z-20 flex h-16 shrink-0 items-center justify-between border-b border-surface/40 bg-base/95 px-4 backdrop-blur-md"
	>
		<div class="flex min-w-0 items-center gap-3">
			<AcerolaButtonIcon onclick={leaveReader} variant="ghost" title="Back">
				<ArrowLeft size={20} />
			</AcerolaButtonIcon>

			<div class="min-w-0">
				<p class="truncate text-sm font-black">{chapter?.name ?? 'Capitulo indisponivel'}</p>
				{#if reader.session}
					<p class="text-xs text-overlay">
						{reader.currentPage + 1} / {reader.pageCount}
					</p>
				{/if}
			</div>
		</div>

		<div class="flex items-center gap-2">
			<AcerolaButtonIcon
				variant="ghost"
				disabled={!reader.session || reader.currentPage <= 0}
				onclick={() => reader.previousPage()}
				title="Previous page"
			>
				<ChevronLeft size={20} />
			</AcerolaButtonIcon>

			<AcerolaButtonIcon
				variant="ghost"
				disabled={!reader.session || reader.currentPage >= reader.pageCount - 1}
				onclick={() => reader.nextPage()}
				title="Next page"
			>
				<ChevronRight size={20} />
			</AcerolaButtonIcon>
		</div>
	</header>

	<main class="scrollbar-hide flex-1 overflow-y-auto bg-mantle/30 px-3 py-6">
		{#if openFailed || !chapter}
			<div class="flex h-full items-center justify-center text-sm font-black text-overlay uppercase">
				Capitulo indisponivel
			</div>
		{:else if reader.pageCount === 0}
			<div class="flex h-full items-center justify-center">
				<RefreshCw size={40} class="animate-spin text-primary" />
			</div>
		{:else}
			<div class="mx-auto flex w-full max-w-6xl flex-col items-center gap-6">
				{#each Array.from({ length: reader.pageCount }) as _, pageIndex}
					{@const pageItem = reader.pageAt(pageIndex)}

					<section
						use:trackPage={pageIndex}
						class="flex min-h-[72vh] w-full items-center justify-center"
						aria-label={`Page ${pageIndex + 1}`}
					>
						{#if pageItem}
							<img
								in:fade={{ duration: 120 }}
								src={pageItem.url}
								alt={`Page ${pageIndex + 1}`}
								class="max-h-none max-w-full bg-base object-contain shadow-2xl shadow-base/40"
								loading={Math.abs(pageIndex - reader.currentPage) <= 2 ? 'eager' : 'lazy'}
								draggable="false"
							/>
						{:else}
							<div
								class="flex h-[72vh] w-full max-w-4xl items-center justify-center border border-surface/40 bg-base/50"
							>
								<RefreshCw size={32} class="animate-spin text-primary" />
							</div>
						{/if}
					</section>
				{/each}
			</div>
		{/if}
	</main>
</div>
