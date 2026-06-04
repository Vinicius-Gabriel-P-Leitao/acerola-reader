<script module lang="ts">
	import type { ReaderCachedPage } from '$lib/contracts/reader/reader.payloads';
	import type { Action } from 'svelte/action';
	import type { ReaderMode } from '../hooks/use-reader-zoom.svelte';

	export type ReaderPageTracker = Action<HTMLElement, number>;

	export type ReaderPagesProps = {
		mode: ReaderMode;
		pageCount: number;
		currentPage: number;
		openFailed: boolean;
		chapterAvailable: boolean;
		pageAt: (index: number) => ReaderCachedPage | undefined;
		trackPage: ReaderPageTracker;
	};
</script>

<script lang="ts">
	import { cn } from '$lib/utils/cn.utils';
	import RefreshCw from '@lucide/svelte/icons/refresh-cw';
	import { fade } from 'svelte/transition';

	let {
		mode,
		pageCount,
		currentPage,
		openFailed,
		chapterAvailable,
		pageAt,
		trackPage
	}: ReaderPagesProps = $props();
</script>

{#if openFailed || !chapterAvailable}
	<div class="text-overlay flex h-full items-center justify-center text-sm font-black uppercase">
		Capitulo indisponivel
	</div>
{:else if pageCount === 0}
	<div class="flex h-full items-center justify-center">
		<RefreshCw size={40} class="animate-spin text-primary" />
	</div>
{:else}
	<div
		class={cn(
			mode === 'horizontal' && 'flex h-full w-max',
			mode === 'webtoon' && 'mx-auto flex w-full max-w-3xl flex-col items-center gap-0 py-2',
			mode === 'vertical' && 'mx-auto flex h-full w-full max-w-6xl flex-col items-center'
		)}
	>
		{#each Array.from({ length: pageCount }) as _, pageIndex}
			{@const pageItem = pageAt(pageIndex)}

			<section
				use:trackPage={pageIndex}
				class={cn(
					mode === 'horizontal' &&
						'flex h-full w-screen shrink-0 snap-center items-center justify-center px-5 py-6',
					mode === 'webtoon' && 'flex w-full items-start justify-center',
					mode === 'vertical' &&
						'flex h-full w-full shrink-0 snap-center items-center justify-center px-3 py-6'
				)}
				aria-label={`Pagina ${pageIndex + 1}`}
			>
				{#if pageItem}
					<div
						class={cn(
							mode === 'webtoon' && 'mx-auto flex w-full justify-center',
							mode !== 'webtoon' && 'flex h-full w-full items-center justify-center'
						)}
					>
						<img
							in:fade={{ duration: 120 }}
							src={pageItem.url}
							alt={`Pagina ${pageIndex + 1}`}
							class={cn(
								'bg-base object-contain',
								mode === 'webtoon' && 'w-full',
								mode !== 'webtoon' && 'max-h-full max-w-full shadow-2xl shadow-base/40'
							)}
							loading={Math.abs(pageIndex - currentPage) <= 2 ? 'eager' : 'lazy'}
							draggable="false"
						/>
					</div>
				{:else}
					<div
						class={cn(
							mode !== 'webtoon' &&
								'flex h-full w-full max-w-4xl items-center justify-center border border-surface/40 bg-base/50',
							mode === 'webtoon' &&
								'flex min-h-[64vh] w-full items-center justify-center border-y border-surface/40 bg-base/50'
						)}
					>
						<RefreshCw size={32} class="animate-spin text-primary" />
					</div>
				{/if}
			</section>
		{/each}
	</div>
{/if}
