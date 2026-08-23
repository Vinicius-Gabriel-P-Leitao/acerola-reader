<script lang="ts">
	import type { ComicSummaryItemPayload } from '$lib/contracts/home/home.payloads';
	import { setComicContext, type ActiveComicState } from '$lib/state/comic-context.svelte';
	import Child from './comic-context-child.svelte';

	let {
		comic,
		cover,
		onReady
	}: {
		comic: ComicSummaryItemPayload;
		cover: string | null;
		onReady: (context: ActiveComicState) => void;
	} = $props();

	const state = setComicContext();

	(() => {
		state.set(comic, cover);
	})();
</script>

<Child {onReady} />
