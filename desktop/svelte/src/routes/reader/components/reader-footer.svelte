<script module lang="ts">
	import type { ReaderMode } from '../hooks/use-reader-zoom.svelte';

	export type ReaderFooterProps = {
		data: {
			pageProgressPercent: number;
			pageProgressWidth: string;
			chapterProgressLabel: string;
			modeLabel: string;
			zoomStatusLabel: string;
			chaptersRemainingLabel: string;
		};
		state: {
			readingMode: ReaderMode;
		};
		events: {
			onReadingModeChange: (mode: ReaderMode) => void;
		};
	};
</script>

<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import ReaderModeToggle from './reader-mode-toggle.svelte';

	let { data, events, state }: ReaderFooterProps = $props();
</script>

<footer
	class="relative z-20 shrink-0 border-t border-surface/40 bg-base/95 px-4 py-3 backdrop-blur-md"
>
	<div class="flex flex-col gap-3">
		<div
			class="text-overlay flex items-center justify-between gap-3 text-[10px] font-black tracking-widest uppercase"
		>
			<span class="shrink-0"
				>{m['pages.reader.progress.read_percent']({ percent: data.pageProgressPercent })}</span
			>
			<span class="hidden min-w-0 truncate md:inline"
				>{data.modeLabel} - {data.zoomStatusLabel}</span
			>
			<span class="min-w-0 truncate text-right">{data.chaptersRemainingLabel}</span>
		</div>

		<div
			role="progressbar"
			aria-valuemin="0"
			aria-valuemax="100"
			aria-valuenow={data.pageProgressPercent}
			class="h-2 overflow-hidden rounded-full bg-surface/60"
			title={data.chapterProgressLabel}
		>
			<div
				class="h-full rounded-full bg-primary transition-[width] duration-300 ease-out"
				style:width={data.pageProgressWidth}
			></div>
		</div>

		<ReaderModeToggle
			state={{ value: state.readingMode }}
			events={{ onValueChange: events.onReadingModeChange }}
			ui={{ variant: 'mobile' }}
		/>
	</div>
</footer>
