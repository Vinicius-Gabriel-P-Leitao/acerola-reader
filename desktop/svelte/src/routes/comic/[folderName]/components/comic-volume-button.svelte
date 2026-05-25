<script lang="ts">
	import Folder from '@lucide/svelte/icons/folder';
	import ChevronDown from '@lucide/svelte/icons/chevron-down';
	import { cn } from '$lib/utils/cn.utils';

	let {
		title,
		totalChapters,
		viewMode = 'cover',
		coverUri = null,
		bannerUri = null,
		isExpanded = false,
		onclick
	}: {
		title: string;
		totalChapters: number;
		viewMode?: 'cover' | 'banner';
		coverUri?: string | null;
		bannerUri?: string | null;
		isExpanded?: boolean;
		onclick: () => void;
	} = $props();

	const activeBanner = $derived(viewMode === 'banner' ? bannerUri || coverUri : null);
	const activeCover = $derived(viewMode === 'cover' ? coverUri || bannerUri : null);
</script>

<button
	{onclick}
	class="group relative flex w-full cursor-pointer items-center justify-between overflow-hidden rounded-3xl border border-surface/40 bg-mantle/40 p-6 text-left transition-colors hover:border-primary/50"
>
	{#if activeBanner}
		<div class="pointer-events-none absolute inset-0 z-0">
			<img
				src={activeBanner}
				alt={title}
				class="h-full w-full object-cover opacity-30 transition-opacity group-hover:opacity-50"
			/>
			<div class="absolute inset-0 bg-linear-to-r from-mantle via-mantle/80 to-transparent"></div>
		</div>
	{/if}

	<div class="relative z-10 flex items-center gap-4">
		{#if viewMode === 'cover' || (viewMode === 'banner' && !activeBanner)}
			<div
				class="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-muted text-foreground transition-colors group-hover:text-primary"
			>
				{#if activeCover}
					<img src={activeCover} alt={title} class="h-full w-full object-cover" />
				{:else}
					<Folder size={28} />
				{/if}
			</div>
		{/if}

		<div>
			<h3 class="text-lg font-bold text-foreground">{title}</h3>
			<p class="text-sm text-muted-foreground">{totalChapters} Capítulos inclusos</p>
		</div>
	</div>

	<div
		class={cn(
			'relative z-10 ml-4 shrink-0 transition-transform duration-300',
			isExpanded && 'rotate-180'
		)}
	>
		<ChevronDown size={20} />
	</div>
</button>
