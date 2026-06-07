<script lang="ts">
	import { onMount } from 'svelte';
	import { useReaderZoom } from './use-reader-zoom.svelte';

	let {
		onReady
	}: {
		onReady: (hook: ReturnType<typeof useReaderZoom>) => void;
	} = $props();

	const hook = useReaderZoom();

	onMount(() => {
		onReady(hook);
	});
</script>
