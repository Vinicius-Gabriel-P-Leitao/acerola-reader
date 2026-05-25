<script module lang="ts">
	import type { Snippet } from 'svelte';

	export type AcerolaCardProps = {
		title: string;
		description?: string;
		children?: Snippet;
		footer?: Snippet;
		size?: 'default' | 'sm';
		class?: string;
	};
</script>

<script lang="ts">
	import * as Card from '$lib/components/ui/card';

	let {
		title,
		description,
		children,
		footer,
		size = 'default',
		class: className
	}: AcerolaCardProps = $props();
</script>

<Card.Root {size} class={className}>
	<Card.Header>
		<Card.Title class="font-semibold text-foreground">{title}</Card.Title>
		{#if description}
			<Card.Description>{description}</Card.Description>
		{/if}
	</Card.Header>

	{#if children}
		<Card.Content>
			{@render children()}
		</Card.Content>
	{/if}

	{#if footer}
		<Card.Footer>
			{@render footer()}
		</Card.Footer>
	{/if}
</Card.Root>
