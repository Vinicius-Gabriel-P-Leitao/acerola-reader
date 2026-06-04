<script module lang="ts">
	import type { Snippet } from 'svelte';
	import type { ReaderMode, ReaderZoomController } from '../hooks/use-reader-zoom.svelte';

	export type ReaderViewportProps = {
		mode: ReaderMode;
		zoom: ReaderZoomController;
		children: Snippet;
	};
</script>

<script lang="ts">
	import { cn } from '$lib/utils/cn.utils';

	let { mode, zoom, children }: ReaderViewportProps = $props();

	function mountViewport(node: HTMLElement) {
		zoom.setViewport(node);

		return {
			destroy() {
				zoom.setViewport(null);
			}
		};
	}
</script>

<main
	use:mountViewport
	class={cn(
		'scrollbar-hide flex-1 bg-mantle/30',
		zoom.isZoomed && 'touch-none overflow-hidden select-none',
		!zoom.isZoomed &&
			mode === 'horizontal' &&
			'snap-x snap-mandatory overflow-x-auto overflow-y-hidden scroll-smooth',
		!zoom.isZoomed &&
			mode === 'vertical' &&
			'snap-y snap-mandatory overflow-x-hidden overflow-y-auto scroll-smooth',
		!zoom.isZoomed && mode === 'webtoon' && 'overflow-x-hidden overflow-y-auto scroll-smooth',
		zoom.isPanning && 'cursor-grabbing',
		!zoom.isPanning && zoom.isZoomed && 'cursor-grab',
		!zoom.isPanning && !zoom.isZoomed && zoom.zoomMode && 'cursor-zoom-in'
	)}
	onwheel={zoom.handleWheel}
	onpointerdown={zoom.handlePointerDown}
	onpointermove={zoom.handlePointerMove}
	onpointerup={zoom.stopPan}
	onpointercancel={zoom.stopPan}
	onlostpointercapture={zoom.stopPan}
	ondblclick={(event) => zoom.toggleQuickZoom(event)}
>
	<div
		class={cn(
			'will-change-transform',
			mode === 'horizontal' && 'h-full w-max',
			mode === 'webtoon' && 'w-full',
			mode === 'vertical' && 'h-full w-full',
			!zoom.isPanning && 'transition-transform duration-150 ease-out'
		)}
		style={zoom.zoomLayerStyle}
	>
		{@render children()}
	</div>
</main>
