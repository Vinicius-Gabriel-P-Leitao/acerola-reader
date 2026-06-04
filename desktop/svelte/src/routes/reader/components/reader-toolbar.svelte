<script module lang="ts">
	import type { ReaderMode } from '../hooks/use-reader-zoom.svelte';

	export type ReaderToolbarProps = {
		title: string;
		subtitle?: string;
		readingMode?: ReaderMode;
		zoomLevel: number;
		zoomMode: boolean;
		isPaginatedMode: boolean;
		pageControlsDisabled: boolean;
		canPreviousPage: boolean;
		canNextPage: boolean;
		onBack: () => void;
		onToggleQuickZoom: () => void;
		onToggleZoomMode: () => void;
		onOpenCommandPalette: () => void;
		onPreviousPage: () => void | Promise<void>;
		onNextPage: () => void | Promise<void>;
	};
</script>

<script lang="ts">
	import AcerolaButtonIcon from '$lib/components/acerola-button/acerola-button-icon.svelte';
	import { m } from '$lib/paraglide/messages';
	import ArrowLeft from '@lucide/svelte/icons/arrow-left';
	import ChevronLeft from '@lucide/svelte/icons/chevron-left';
	import ChevronRight from '@lucide/svelte/icons/chevron-right';
	import CommandIcon from '@lucide/svelte/icons/command';
	import ZoomIn from '@lucide/svelte/icons/zoom-in';
	import ZoomOut from '@lucide/svelte/icons/zoom-out';
	import ReaderModeToggle from './reader-mode-toggle.svelte';

	let {
		title,
		subtitle,
		readingMode = $bindable<ReaderMode>('vertical'),
		zoomLevel,
		zoomMode,
		isPaginatedMode,
		pageControlsDisabled,
		canPreviousPage,
		canNextPage,
		onBack,
		onToggleQuickZoom,
		onToggleZoomMode,
		onOpenCommandPalette,
		onPreviousPage,
		onNextPage
	}: ReaderToolbarProps = $props();
</script>

<header
	class="relative z-20 flex min-h-16 shrink-0 items-center justify-between gap-3 border-b border-surface/40 bg-base/95 px-4 py-2 backdrop-blur-md"
>
	<div class="flex min-w-0 flex-1 items-center gap-3">
		<AcerolaButtonIcon onclick={onBack} variant="ghost" title={m['pages.reader.actions.back']()}>
			<ArrowLeft size={20} />
		</AcerolaButtonIcon>

		<div class="min-w-0">
			<p class="truncate text-sm font-black">{title}</p>
			{#if subtitle}
				<p class="text-overlay truncate text-xs">{subtitle}</p>
			{/if}
		</div>
	</div>

	<ReaderModeToggle bind:value={readingMode} />

	<div class="flex shrink-0 items-center gap-1">
		<AcerolaButtonIcon
			variant={zoomLevel > 1 ? 'secondary' : 'ghost'}
			onclick={onToggleQuickZoom}
			title={zoomLevel > 1
				? m['pages.reader.actions.reset_zoom']()
				: m['pages.reader.actions.apply_zoom']()}
		>
			{#if zoomLevel > 1}
				<ZoomOut size={20} />
			{:else}
				<ZoomIn size={20} />
			{/if}
		</AcerolaButtonIcon>

		<AcerolaButtonIcon
			variant={zoomMode ? 'default' : 'ghost'}
			onclick={onToggleZoomMode}
			title={m['pages.reader.actions.zoom_mode']()}
		>
			<ZoomIn size={20} />
		</AcerolaButtonIcon>

		<AcerolaButtonIcon
			variant="ghost"
			onclick={onOpenCommandPalette}
			title={m['pages.reader.actions.commands']()}
		>
			<CommandIcon size={20} />
		</AcerolaButtonIcon>

		{#if isPaginatedMode}
			<AcerolaButtonIcon
				variant="ghost"
				disabled={!canPreviousPage || pageControlsDisabled}
				onclick={() => onPreviousPage()}
				title={pageControlsDisabled
					? m['pages.reader.actions.page_navigation_locked']()
					: m['pages.reader.actions.previous_page']()}
			>
				<ChevronLeft size={20} />
			</AcerolaButtonIcon>

			<AcerolaButtonIcon
				variant="ghost"
				disabled={!canNextPage || pageControlsDisabled}
				onclick={() => onNextPage()}
				title={pageControlsDisabled
					? m['pages.reader.actions.page_navigation_locked']()
					: m['pages.reader.actions.next_page']()}
			>
				<ChevronRight size={20} />
			</AcerolaButtonIcon>
		{/if}
	</div>
</header>
