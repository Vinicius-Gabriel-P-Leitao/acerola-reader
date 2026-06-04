<script lang="ts">
	import { onMount } from 'svelte';
	import { useComicChapters } from './use-comic-chapters.svelte';

	let {
		onReady
	}: {
		onReady: (hook: ReturnType<typeof useComicChapters>) => void;
	} = $props();

	const hook = useComicChapters();

	onMount(() => {
		onReady(hook);
	});
</script>
