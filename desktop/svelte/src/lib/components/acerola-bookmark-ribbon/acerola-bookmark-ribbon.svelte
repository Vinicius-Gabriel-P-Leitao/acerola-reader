<script lang="ts">
	import { cn } from '$lib/utils/cn.utils';
	import { m } from '$lib/paraglide/messages';

	/**
	 * Props for the BookmarkRibbon component.
	 */
	export type AcerolaBookmarkRibbonProps = {
		/** The color of the bookmark ribbon as an integer (e.g. 0xFF0000) */
		color: number;
		/** Name of the bookmark category, used as the accessible label since color alone isn't distinguishable */
		name?: string;
		/** Optional CSS class to apply to the ribbon */
		class?: string;
	};

	let { color, name, class: className }: AcerolaBookmarkRibbonProps = $props();

	// Remove the alpha channel 'FF' at the beginning if present, or just use as is
	// But since it's 0xFFF44336, we can bitwise AND with 0xFFFFFF to get RGB.
	// In JS, numbers are double precision, bitwise operates on 32-bit signed integers.
	// 0xFFF44336 & 0xFFFFFF = 0xF44336
	let hexColor = $derived('#' + (color & 0xffffff).toString(16).padStart(6, '0'));
	let label = $derived(
		name ? m['components.bookmark_ribbon.label']({ name }) : m['components.bookmark_ribbon.default_label']()
	);
</script>

<div
	role="img"
	aria-label={label}
	title={label}
	class={cn('absolute -top-2.5 left-3 z-10 h-8 w-5 drop-shadow-md', className)}
	style="background-color: {hexColor}; clip-path: polygon(0 0, 100% 0, 100% 100%, 50% 75%, 0 100%);"
></div>
