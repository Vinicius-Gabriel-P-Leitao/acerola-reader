<script lang="ts" module>
	import { type NavDockMode } from '$lib/hooks/preferences/use-nav-dock-mode.svelte';
	import { m } from '$lib/paraglide/messages';

	export type DockModePickerOption = {
		id: NavDockMode;
		name: () => string;
		description: () => string;
	};

	const modes: DockModePickerOption[] = [
		{
			id: 'fixed',
			name: m['dock_mode.fixed.name'],
			description: m['dock_mode.fixed.desc']
		},
		{
			id: 'hover',
			name: m['dock_mode.hover.name'],
			description: m['dock_mode.hover.desc']
		}
	];

	export type DockModePickerProps = {
		data: {
			mode: NavDockMode;
		};
		ui?: {
			showHeader?: boolean;
		};
		events: {
			onSelect: (mode: NavDockMode) => void;
		};
	};
</script>

<script lang="ts">
	import CheckIcon from '@lucide/svelte/icons/check';
	import DockIcon from '@lucide/svelte/icons/dock';

	let { data, ui = { showHeader: true }, events }: DockModePickerProps = $props();
	let showHeader = $derived(ui.showHeader ?? true);
</script>

<section class="space-y-4">
	{#if showHeader}
		<div
			class="flex items-center gap-3 text-xs font-bold tracking-widest text-muted-foreground uppercase"
		>
			<DockIcon size={16} />
			{m['pages.config.components.dock_mode']()}
		</div>
	{/if}

	<div class="grid grid-cols-1 gap-3.5 sm:grid-cols-2">
		{#each modes as it}
			<button
				type="button"
				onclick={() => events.onSelect(it.id)}
				class="group relative flex flex-col justify-between cursor-pointer overflow-hidden rounded-2xl border-2 p-4 text-left transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/50 active:scale-[0.98]
          {data.mode === it.id
					? 'border-primary bg-primary/5 shadow-md shadow-primary/10'
					: 'border-border/60 bg-card hover:border-muted-foreground/50 hover:bg-card/90 hover:shadow-sm'}"
			>
				<div>
					<div
						class="relative mb-3 h-16 w-full overflow-hidden rounded-lg border border-border/40 bg-muted/30"
					>
						<div class="absolute inset-x-0 bottom-2 flex justify-center">
							{#if it.id === 'fixed'}
								<div class="h-3 w-2/3 rounded-full bg-primary/60 transition-transform group-hover:scale-105"
								></div>
							{:else}
								<div class="h-1 w-6 rounded-full bg-primary/40 transition-transform group-hover:scale-105"
								></div>
							{/if}
						</div>
					</div>

					<h3 class="text-sm font-bold text-foreground transition-colors group-hover:text-primary">
						{it.name()}
					</h3>
					<p class="mt-1 text-xs leading-snug text-muted-foreground line-clamp-2">
						{it.description()}
					</p>
				</div>

				{#if data.mode === it.id}
					<div
						class="absolute top-3.5 right-3.5 flex h-5 w-5 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-sm"
					>
						<CheckIcon size={12} strokeWidth={3} />
					</div>
				{/if}
			</button>
		{/each}
	</div>
</section>
