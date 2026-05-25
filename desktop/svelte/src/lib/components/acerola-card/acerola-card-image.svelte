<script module lang="ts">
	import type { Snippet } from 'svelte';

	export type AcerolaCardImageProps = {
		data: {
			title: string;
			cover?: string | null;
			progress?: number;
			description?: string;
		};
		events?: {
			onClick?: (event: MouseEvent) => void;
		};
		ui?: {
			class?: string;
		};
	};

	export type AcerolaCardImageSnippets = {
		placeholder?: Snippet;
		overlay?: Snippet;
		footer?: Snippet;
		action?: Snippet;
	};
</script>

<script lang="ts">
	import BookOpenIcon from '@lucide/svelte/icons/book-open';
	import { AspectRatio } from '$lib/components/ui/aspect-ratio';
	import { cn } from '$lib/utils/cn.utils';

	let {
		ui,
		data,
		events,
		footer,
		action,
		overlay,
		placeholder
	}: AcerolaCardImageProps & AcerolaCardImageSnippets = $props();
</script>

<div
	class={cn(
		'group w-36 transition-transform duration-300 hover:-translate-y-2 hover:scale-[1.02]',
		ui?.class
	)}
>
	<AspectRatio
		ratio={2 / 3}
		class="overflow-hidden rounded-xl bg-surface shadow-lg transition-shadow duration-300 group-hover:shadow-2xl group-hover:shadow-primary/20"
	>
		<button
			type="button"
			class="relative h-full w-full cursor-pointer text-left"
			onclick={events?.onClick}
		>
			{#if data.cover}
				<img
					src={data.cover}
					alt={data.title}
					class="h-full w-full object-cover object-top transition-transform duration-700 group-hover:scale-110"
				/>
			{:else if placeholder}
				{@render placeholder()}
			{:else}
				<div
					class="flex h-full w-full items-center justify-center bg-surface text-muted-foreground"
				>
					<BookOpenIcon size={40} />
				</div>
			{/if}

			<div
				class="absolute inset-0 flex flex-col justify-end bg-linear-to-t from-crust via-crust/30 to-transparent p-4 opacity-0 transition-opacity duration-300 group-hover:opacity-100"
			>
				{#if overlay}
					{@render overlay()}
				{:else if data.description}
					<p
						class="line-clamp-3 translate-y-2 text-xs text-foreground transition-transform duration-300 group-hover:translate-y-0"
					>
						{data.description}
					</p>
				{/if}
			</div>

			{#if data.progress !== undefined}
				<div class="absolute bottom-0 left-0 h-1 w-full bg-surface">
					<div
						class="h-full bg-primary transition-all duration-500"
						style={`width:${Math.min(100, Math.max(0, data.progress))}%`}
					></div>
				</div>
			{/if}
		</button>
	</AspectRatio>

	<div class="mt-3 flex items-start justify-between gap-2 px-1">
		<div class="min-w-0 flex-1">
			<h3
				class="line-clamp-1 text-sm font-bold transition-colors duration-200 group-hover:text-primary"
			>
				{data.title}
			</h3>

			{#if footer}
				<div class="mt-1">
					{@render footer()}
				</div>
			{/if}
		</div>

		{#if action}
			{@render action()}
		{/if}
	</div>
</div>
