<script lang="ts">
	import { onMount } from 'svelte';
	import { useComicContext, type ActiveComicState } from './comic-context.svelte';

	let {
		onReady
	}: {
		onReady: (context: ActiveComicState) => void;
	} = $props();

	const context = useComicContext();

	onMount(() => {
		onReady(context);
	});
</script>
