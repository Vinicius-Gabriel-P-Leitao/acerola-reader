<script module lang="ts">
	import type { Snippet } from 'svelte';

	export type AcerolaHeroButtonProps = {
		title?: string;
		class?: string;
		description?: string;
		icon?: Snippet;
		action?: Snippet;
		children?: Snippet;
		onclick?: (event: MouseEvent) => void;
	};
</script>

<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import * as Item from '$lib/components/ui/item/index.js';
	import { cn } from '$lib/utils/cn.utils';

	let {
		title,
		description,
		class: className,
		icon,
		action,
		onclick,
		...rest
	}: AcerolaHeroButtonProps = $props();
</script>

<Item.Root
	class={cn(
		'group flex items-center justify-between rounded-3xl border border-border bg-card p-6 transition-colors',
		onclick ? 'cursor-pointer hover:border-primary/50' : '',
		className
	)}
	{onclick}
	{...rest}
>
	<div class="flex items-center gap-4">
		{#if icon}
			<Item.Media
				class="flex h-12 w-12 items-center justify-center rounded-2xl bg-muted text-foreground transition-colors group-hover:text-primary"
			>
				{@render icon()}
			</Item.Media>
		{/if}

		<Item.Content class="text-left">
			<Item.Title class="text-lg font-bold text-foreground">
				{title ?? m['components.hero_button.default_title']()}
			</Item.Title>

			{#if description}
				<Item.Description class="text-sm text-muted-foreground">{description}</Item.Description>
			{/if}
		</Item.Content>
	</div>

	{#if action}
		<Item.Actions class="ml-4 shrink-0">
			{@render action()}
		</Item.Actions>
	{/if}
</Item.Root>
