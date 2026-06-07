<script lang="ts">
	import type { ComicSummaryItem } from '$lib/contracts/home/home.payloads';
	import Child from './comic-context-child.test-harness.svelte';
	import { setComicContext, type ActiveComicState } from './comic-context.svelte';

	let {
		comic,
		cover,
		onReady
	}: {
		comic: ComicSummaryItem;
		cover: string | null;
		onReady: (context: ActiveComicState) => void;
	} = $props();

	const state = setComicContext();

	(() => {
		state.set(comic, cover);
	})();
</script>

<Child {onReady} />
