<script module lang="ts">
	import type { Component, Snippet } from 'svelte';

	export type DockItem = {
		href: string;
		label: string;
		icon: Component;
	};

	export type AcerolaDockProps = {
		data: {
			items: DockItem[];
		};
		state: {
			mode: 'fixed' | 'hover';
		};
	};

	export type AcerolaDockSnippets = {
		brand?: Snippet;
	};
</script>

<script lang="ts">
	import { page } from '$app/state';
	import * as Tooltip from '$lib/components/ui/tooltip/index.js';
	import { Separator } from '$lib/components/ui/separator/index.js';
	import { fade, scale } from 'svelte/transition';
	import { cubicOut } from 'svelte/easing';

	let { data, state: control, brand }: AcerolaDockProps & AcerolaDockSnippets = $props();

	let isExpanded = $state(false);
	let collapseTimeout: ReturnType<typeof setTimeout> | undefined;

	const isOpen = $derived(control.mode === 'fixed' || isExpanded);

	function handleMouseEnter() {
		clearTimeout(collapseTimeout);
		isExpanded = true;
	}

	function handleMouseLeave() {
		clearTimeout(collapseTimeout);
		collapseTimeout = setTimeout(() => {
			isExpanded = false;
		}, 250);
	}
</script>

<Tooltip.Provider delayDuration={200}>
	<div class="pointer-events-none fixed inset-x-0 bottom-0 z-40 flex justify-center pb-4">
		<div
			class="pointer-events-auto grid"
			role="navigation"
			onmouseenter={control.mode === 'hover' ? handleMouseEnter : undefined}
			onmouseleave={control.mode === 'hover' ? handleMouseLeave : undefined}
		>
			{#if isOpen}
				<div
					in:scale={{ start: 0.9, duration: 180, easing: cubicOut }}
					out:fade={{ duration: 120 }}
					class="col-start-1 row-start-1 flex origin-bottom items-center gap-1 rounded-full border border-surface/30 bg-surface/20 p-2 shadow-2xl backdrop-blur-xl"
				>
					{#if brand}
						<div class="flex size-9 shrink-0 items-center justify-center overflow-hidden rounded-full">
							{@render brand()}
						</div>
						<Separator orientation="vertical" class="mx-1 h-6" />
					{/if}

					{#each data.items as item (item.href)}
						<Tooltip.Root>
							<Tooltip.Trigger>
								{#snippet child({ props })}
									<a
										href={item.href}
										{...props}
										aria-label={item.label}
										class="flex size-11 shrink-0 items-center justify-center rounded-full transition-colors [&_svg]:size-5 {page
											.url.pathname === item.href
											? 'bg-primary text-primary-foreground shadow-lg'
											: 'text-foreground hover:bg-surface/80'}"
									>
										<item.icon />
									</a>
								{/snippet}
							</Tooltip.Trigger>
							<Tooltip.Content side="top">{item.label}</Tooltip.Content>
						</Tooltip.Root>
					{/each}
				</div>
			{:else}
				<div
					in:fade={{ delay: 120, duration: 120 }}
					class="col-start-1 row-start-1 flex items-center justify-center p-4"
				>
					<div class="h-1.5 w-12 rounded-full bg-muted-foreground/70 shadow-sm transition-all duration-200"
					></div>
				</div>
			{/if}
		</div>
	</div>
</Tooltip.Provider>
