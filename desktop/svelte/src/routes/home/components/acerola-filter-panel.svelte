<script module lang="ts">
	import type { MetadataSource, SortBy, SortOrder } from '$lib/contracts/home/home.payloads';

	export type FilterPanelProps = {
		state?: { open?: boolean };
		data: {
			sortBy: SortBy;
			sortOrder: SortOrder;
			showHidden: boolean;
			metadataSource: MetadataSource;
		};
		events: {
			onApply: (params: {
				sortBy: SortBy;
				sortOrder: SortOrder;
				showHidden: boolean;
				metadataSource: MetadataSource;
			}) => void;
			onClose: () => void;
		};
	};
</script>

<script lang="ts">
	import { untrack } from 'svelte';
	import { fly, fade } from 'svelte/transition';
	import { cubicOut } from 'svelte/easing';
	import ArrowUp from '@lucide/svelte/icons/arrow-up';
	import ArrowDown from '@lucide/svelte/icons/arrow-down';
	import X from '@lucide/svelte/icons/x';
	import Check from '@lucide/svelte/icons/check';
	import Eye from '@lucide/svelte/icons/eye';
	import AcerolaSwitch from '$lib/components/acerola-switch/acerola-switch.svelte';

	let { state: controlState, data, events }: FilterPanelProps = $props();

	let localSortBy = $state<SortBy>('title');
	let localSortOrder = $state<SortOrder>('asc');
	let localShowHidden = $state(false);
	let localMetadataSource = $state<MetadataSource>('all');
	let wasOpen = false;

	$effect(() => {
		const isOpen = Boolean(controlState?.open);
		if (isOpen && !wasOpen) {
			localSortBy = data.sortBy;
			localSortOrder = data.sortOrder;
			localShowHidden = data.showHidden;
			localMetadataSource = data.metadataSource;
		}
		wasOpen = isOpen;
	});

	const sortOptions: { value: SortBy; labelKey: string }[] = [
		{ value: 'title', labelKey: 'Título' },
		{ value: 'chapterCount', labelKey: 'Quantidade de Capítulos' },
		{ value: 'lastUpdated', labelKey: 'Última Atualização' }
	];

	const metadataSources: { value: MetadataSource; label: string }[] = [
		{ value: 'all', label: 'Todos' },
		{ value: 'comicinfo', label: 'ComicInfo' },
		{ value: 'mangadex', label: 'MangaDex' },
		{ value: 'anilist', label: 'AniList' },
		{ value: 'no_metadata', label: 'Sem metadados' }
	];

	function selectSort(value: SortBy) {
		if (localSortBy === value) {
			localSortOrder = localSortOrder === 'asc' ? 'desc' : 'asc';
		} else {
			localSortBy = value;
			localSortOrder = 'asc';
		}
	}

	function handleApply() {
		events.onApply({
			sortBy: localSortBy,
			sortOrder: localSortOrder,
			showHidden: localShowHidden,
			metadataSource: localMetadataSource
		});
	}

	function handleReset() {
		localSortBy = 'title';
		localSortOrder = 'asc';
		localShowHidden = false;
		localMetadataSource = 'all';
	}

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Escape' && controlState?.open) {
			events.onClose();
		}
	}

	const hasChanges = $derived(
		localSortBy !== data.sortBy ||
			localSortOrder !== data.sortOrder ||
			localShowHidden !== data.showHidden ||
			localMetadataSource !== data.metadataSource
	);

	const hasActiveFilters = $derived(localShowHidden || localMetadataSource !== 'all');
</script>

<svelte:window onkeydown={handleKeydown} />

{#if controlState?.open}
	<!-- Backdrop -->
	<div
		role="presentation"
		class="fixed inset-x-0 top-8 bottom-0 z-40 bg-background/40 backdrop-blur-sm"
		transition:fade={{ duration: 200 }}
		onclick={events.onClose}
	></div>

	<!-- Panel -->
	<div
		role="dialog"
		aria-modal="true"
		aria-label="Filtrar e Ordenar"
		class="fixed top-8 right-0 bottom-0 z-50 flex w-full max-w-full sm:max-w-sm flex-col border-l border-border/60 bg-background/95 shadow-2xl backdrop-blur-xl"
		transition:fly={{ x: 400, duration: 300, easing: cubicOut }}
	>
		<!-- Header -->
		<div class="flex items-center justify-between border-b border-border/40 px-6 py-5">
			<div class="flex items-center gap-3">
				<div
					class="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/15 text-primary"
				>
					<svg
						xmlns="http://www.w3.org/2000/svg"
						width="16"
						height="16"
						viewBox="0 0 24 24"
						fill="none"
						stroke="currentColor"
						stroke-width="2.5"
						stroke-linecap="round"
						stroke-linejoin="round"
					>
						<line x1="21" y1="4" x2="14" y2="4"></line>
						<line x1="10" y1="4" x2="3" y2="4"></line>
						<line x1="21" y1="12" x2="12" y2="12"></line>
						<line x1="8" y1="12" x2="3" y2="12"></line>
						<line x1="21" y1="20" x2="16" y2="20"></line>
						<line x1="12" y1="20" x2="3" y2="20"></line>
						<line x1="14" y1="2" x2="14" y2="6"></line>
						<line x1="8" y1="10" x2="8" y2="14"></line>
						<line x1="16" y1="18" x2="16" y2="22"></line>
					</svg>
				</div>
				<div>
					<h2 class="text-base font-bold tracking-tight text-foreground">Filtrar e Ordenar</h2>
					{#if hasActiveFilters}
						<span class="text-[10px] font-semibold tracking-wider text-primary uppercase"
							>Filtros ativos</span
						>
					{/if}
				</div>
			</div>
			<button
				type="button"
				aria-label="Fechar painel de filtros"
				class="flex h-8 w-8 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted/80 hover:text-foreground focus-visible:ring-2 focus-visible:ring-primary focus-visible:outline-none"
				onclick={events.onClose}
			>
				<X size={18} />
			</button>
		</div>

		<!-- Content - scrollable -->
		<div class="flex-1 overflow-y-auto px-6 py-5 space-y-7">
			<!-- Sort Section -->
			<section>
				<p class="mb-3 text-[11px] font-bold tracking-widest text-primary uppercase">
					Ordenar por
				</p>
				<div class="space-y-1.5" role="radiogroup" aria-label="Opções de ordenação">
					{#each sortOptions as option}
						{@const isSelected = localSortBy === option.value}
						<button
							type="button"
							role="radio"
							aria-checked={isSelected}
							class="group flex w-full items-center justify-between rounded-xl px-3.5 py-3 text-sm font-medium transition-all duration-200 focus-visible:ring-2 focus-visible:ring-primary focus-visible:outline-none {isSelected
								? 'bg-primary/15 text-foreground ring-1 ring-primary/30'
								: 'text-muted-foreground hover:bg-muted/60 hover:text-foreground'}"
							onclick={() => selectSort(option.value)}
						>
							<span class="truncate">{option.labelKey}</span>
							{#if isSelected}
								<div class="ml-2 flex shrink-0 items-center gap-1.5">
									<div
										class="flex items-center gap-1 rounded-md bg-primary/20 px-2 py-0.5 text-[10px] font-bold text-primary"
									>
										{#if localSortOrder === 'asc'}
											<ArrowUp size={10} />
											<span>A–Z</span>
										{:else}
											<ArrowDown size={10} />
											<span>Z–A</span>
										{/if}
									</div>
									<Check size={14} class="text-primary" />
								</div>
							{/if}
						</button>
					{/each}
				</div>
			</section>

			<!-- Divider -->
			<div class="h-px bg-border/40"></div>

			<!-- Filter Section -->
			<section>
				<p class="mb-3 text-[11px] font-bold tracking-widest text-primary uppercase">
					Filtrar por
				</p>

				<!-- Show Hidden Toggle Row -->
				<button
					type="button"
					role="switch"
					aria-checked={localShowHidden}
					class="flex w-full items-center justify-between rounded-xl px-3.5 py-3 text-left transition-colors hover:bg-muted/40 focus-visible:ring-2 focus-visible:ring-primary focus-visible:outline-none cursor-pointer"
					onclick={() => (localShowHidden = !localShowHidden)}
				>
					<div class="flex items-center gap-3">
						<div
							class="flex h-8 w-8 items-center justify-center rounded-lg bg-muted/60 text-muted-foreground"
						>
							<Eye size={15} />
						</div>
						<div>
							<p class="text-sm font-medium text-foreground">Mostrar ocultos</p>
							<p class="text-xs text-muted-foreground">Exibir quadrinhos ocultos</p>
						</div>
					</div>
					<div class="pointer-events-none">
						<AcerolaSwitch
							state={{ checked: localShowHidden }}
							ui={{ size: 'sm' }}
						/>
					</div>
				</button>
			</section>

			<!-- Metadata Source Section -->
			<section>
				<p class="mb-3 text-[11px] font-bold tracking-widest text-primary uppercase">
					Fontes de Metadados
				</p>
				<div class="flex flex-wrap gap-2" role="group" aria-label="Fontes de metadados">
					{#each metadataSources as source}
						{@const isSelected = localMetadataSource === source.value}
						<button
							type="button"
							aria-pressed={isSelected}
							class="rounded-xl border px-3.5 py-2 text-sm font-semibold transition-all duration-200 active:scale-95 focus-visible:ring-2 focus-visible:ring-primary focus-visible:outline-none {isSelected
								? 'border-primary/50 bg-primary/20 text-primary shadow-sm shadow-primary/20'
								: 'border-border/60 bg-muted/30 text-muted-foreground hover:border-border hover:bg-muted/60 hover:text-foreground'}"
							onclick={() => (localMetadataSource = source.value)}
						>
							{source.label}
						</button>
					{/each}
				</div>
			</section>
		</div>

		<!-- Footer Actions -->
		<div class="border-t border-border/40 px-6 py-5">
			<div class="flex gap-3">
				<button
					type="button"
					class="flex-1 rounded-xl border border-border/60 bg-muted/30 px-4 py-2.5 text-sm font-semibold text-muted-foreground transition-all hover:bg-muted/60 hover:text-foreground active:scale-[0.98] disabled:opacity-40 focus-visible:ring-2 focus-visible:ring-primary focus-visible:outline-none"
					onclick={handleReset}
					disabled={!hasChanges && !hasActiveFilters}
				>
					Resetar
				</button>
				<button
					type="button"
					class="flex-1 rounded-xl bg-primary px-4 py-2.5 text-sm font-bold text-primary-foreground shadow-md shadow-primary/30 transition-all hover:bg-primary/90 hover:shadow-lg hover:shadow-primary/40 active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-primary focus-visible:outline-none"
					onclick={handleApply}
				>
					Aplicar
				</button>
			</div>
		</div>
	</div>
{/if}
