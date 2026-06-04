<script module lang="ts">
	import type { ReaderMode } from '../hooks/use-reader-zoom.svelte';

	export type ReaderModeToggleProps = {
		value?: ReaderMode;
		variant?: 'desktop' | 'mobile';
		class?: string;
	};
</script>

<script lang="ts">
	import AcerolaToggleGroup from '$lib/components/acerola-toggle-group/acerola-toggle-group.svelte';
	import { ToggleGroupItem } from '$lib/components/ui/toggle-group/index.js';
	import { m } from '$lib/paraglide/messages';
	import { cn } from '$lib/utils/cn.utils';
	import Columns2 from '@lucide/svelte/icons/columns-2';
	import Rows2 from '@lucide/svelte/icons/rows-2';
	import ScrollText from '@lucide/svelte/icons/scroll-text';

	let {
		value = $bindable<ReaderMode>('vertical'),
		variant = 'desktop',
		class: className
	}: ReaderModeToggleProps = $props();
</script>

<AcerolaToggleGroup
	type="single"
	bind:value
	class={cn(
		variant === 'desktop' &&
			'hidden shrink-0 gap-1 rounded-xl border border-surface/40 bg-mantle/60 p-1 md:flex',
		variant === 'mobile' &&
			'grid grid-cols-3 gap-1 rounded-xl border border-surface/40 bg-mantle/60 p-1 md:hidden',
		className
	)}
>
	{#snippet children()}
		<ToggleGroupItem
			value="vertical"
			title={m['pages.reader.modes.vertical']()}
			class={cn(
				'h-9 gap-2 rounded-lg text-[10px] font-black tracking-widest uppercase data-[state=on]:bg-primary data-[state=on]:text-primary-foreground',
				variant === 'desktop' && 'px-3'
			)}
		>
			<Rows2 size={15} />
			{#if variant === 'desktop'}
				<span class="hidden lg:inline">{m['pages.reader.modes.vertical_short']()}</span>
			{:else}
				<span>{m['pages.reader.modes.vertical_short']()}</span>
			{/if}
		</ToggleGroupItem>

		<ToggleGroupItem
			value="horizontal"
			title={m['pages.reader.modes.horizontal']()}
			class={cn(
				'h-9 gap-2 rounded-lg text-[10px] font-black tracking-widest uppercase data-[state=on]:bg-primary data-[state=on]:text-primary-foreground',
				variant === 'desktop' && 'px-3'
			)}
		>
			<Columns2 size={15} />
			{#if variant === 'desktop'}
				<span class="hidden lg:inline">{m['pages.reader.modes.horizontal_short']()}</span>
			{:else}
				<span>{m['pages.reader.modes.horizontal_short']()}</span>
			{/if}
		</ToggleGroupItem>

		<ToggleGroupItem
			value="webtoon"
			title={m['pages.reader.modes.webtoon']()}
			class={cn(
				'h-9 gap-2 rounded-lg text-[10px] font-black tracking-widest uppercase data-[state=on]:bg-primary data-[state=on]:text-primary-foreground',
				variant === 'desktop' && 'px-3'
			)}
		>
			<ScrollText size={15} />
			{#if variant === 'desktop'}
				<span class="hidden lg:inline">{m['pages.reader.modes.webtoon']()}</span>
			{:else}
				<span>{m['pages.reader.modes.webtoon']()}</span>
			{/if}
		</ToggleGroupItem>
	{/snippet}
</AcerolaToggleGroup>
