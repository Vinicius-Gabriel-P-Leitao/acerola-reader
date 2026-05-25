<script module lang="ts">
	import type { Snippet } from 'svelte';

	export type AcerolaPopoverProps = {
		trigger: Snippet;
		content: Snippet;
		open?: boolean;
		contentClass?: string;
	};
</script>

<script lang="ts">
	import * as Popover from '$lib/components/ui/popover/index.js';

	let { trigger, content, open = $bindable(false), contentClass }: AcerolaPopoverProps = $props();
</script>

<Popover.Root bind:open>
	<Popover.Trigger>
		{@render trigger()}
	</Popover.Trigger>

	<Popover.Content class={contentClass}>
		{@render content()}
	</Popover.Content>
</Popover.Root>
