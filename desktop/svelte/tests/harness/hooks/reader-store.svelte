<script lang="ts">
	import { onMount } from 'svelte';
	import { useReader } from '$lib/hooks/store/use-reader.svelte';

	let {
		onReady
	}: {
		onReady: (hook: ReturnType<typeof useReader>) => void;
	} = $props();

	const hook = useReader();

	onMount(() => {
		onReady(hook);
	});
</script>
