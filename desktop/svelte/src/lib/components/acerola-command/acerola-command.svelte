<script module lang="ts">
	import type { Snippet } from 'svelte';

	export type AcerolaCommandProps = {
		children: Snippet;
		value?: string;
		onValueChange?: (value: string) => void;
	};
</script>

<script lang="ts">
	import * as Command from '$lib/components/ui/command/index.js';

	let { children, onValueChange, value = $bindable('') }: AcerolaCommandProps = $props();
</script>

<Command.Root bind:value {onValueChange}>
	{@render children()}
</Command.Root>
