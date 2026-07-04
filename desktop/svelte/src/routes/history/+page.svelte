<script lang="ts">
	import { goto } from '$app/navigation';
	import { invoke } from '@tauri-apps/api/core';
	import { onMount } from 'svelte';
	import { m } from '$lib/paraglide/messages';

	import PlaceholderManga from '$lib/assets/placeholder/placeholder_manga.svg?component';
	import AcerolaCardImage from '$lib/components/acerola-card/acerola-card-image.svelte';
	import BookOpen from '@lucide/svelte/icons/book-open';
	import Trash2 from '@lucide/svelte/icons/trash-2';
	import { resolveArtworkPath } from '$lib/utils/artwork.utils';

	type ReadingHistoryView = {
		comicDirectoryId: string;
		chapterArchiveId: string;
		lastPage: number;
		isCompleted: boolean;
		updatedAt: number;
		comicName: string;
		comicCover: string | null;
		chapterName: string;
		folderName: string;
		chapterPath: string;
		chapterSort: string;
		isSpecial: boolean;
		lastModified: number;
	};

	let historyItems = $state<ReadingHistoryView[]>([]);
	let loading = $state(true);

	async function loadHistory() {
		try {
			loading = true;
			historyItems = await invoke<ReadingHistoryView[]>('history_get_all');
		} catch (error) {
			console.error('Failed to load history:', error);
		} finally {
			loading = false;
		}
	}

	async function clearHistory() {
		try {
			await invoke('history_clear');
			historyItems = [];
		} catch (error) {
			console.error('Failed to clear history:', error);
		}
	}

	function resumeReading(item: ReadingHistoryView) {
		goto('/reader', {
			state: {
				comicDirectoryId: item.comicDirectoryId.toString(),
				startPage: item.lastPage,
				chapterScope: item.comicName,
				chapter: {
					id: item.chapterArchiveId.toString(),
					name: item.chapterName,
					path: item.chapterPath,
					chapterSort: item.chapterSort,
					isSpecial: item.isSpecial,
					lastModified: item.lastModified,
					volumeId: null,
					volumeName: null
				}
			}
		});
	}

	function openComic(item: ReadingHistoryView) {
		goto(`/comic/${item.folderName}`, {
			state: {
				comicDirectoryId: item.comicDirectoryId
			}
		});
	}

	onMount(() => {
		loadHistory();
	});
</script>

<div class="flex flex-col h-full overflow-hidden">
	<div class="flex shrink-0 items-center justify-between px-8 py-6">
		<h2 class="text-2xl font-bold tracking-tight">{m['pages.history.title']()}</h2>
		{#if historyItems.length > 0}
			<button
				class="flex items-center gap-2 rounded-md bg-destructive px-3 py-1.5 text-sm font-medium text-destructive-foreground transition-colors hover:bg-destructive/90"
				onclick={clearHistory}
			>
				<Trash2 size={16} />
				{m['pages.history.clear_history'] ? m['pages.history.clear_history']() : 'Clear History'}
			</button>
		{/if}
	</div>

	{#if loading}
		<div class="flex flex-1 items-center justify-center p-8 text-muted-foreground">
			{m['pages.history.loading'] ? m['pages.history.loading']() : 'Loading...'}
		</div>
	{:else if historyItems.length > 0}
		{@const heroItem = historyItems[0]}
		{@const heroCover = resolveArtworkPath(heroItem.comicCover)}
		
		<div class="flex-1 overflow-auto pb-8">
			<div class="relative w-full shrink-0 mb-8 overflow-hidden h-[400px]">
				{#if heroCover}
					<img src={heroCover} class="absolute inset-0 w-full h-full object-cover opacity-20 blur-3xl scale-110" referrerpolicy="no-referrer" alt="background" />
				{:else}
					<div class="absolute inset-0 bg-primary/10"></div>
				{/if}
				<div class="absolute inset-0 bg-gradient-to-t from-base via-base/80 to-transparent"></div>
				
				<div class="absolute bottom-8 left-8 right-8 flex items-end gap-8">
					{#if heroCover}
						<button class="relative w-48 h-64 rounded-xl shadow-2xl ring-1 ring-surface overflow-hidden hover:scale-105 transition-transform" onclick={() => openComic(heroItem)}>
							<img src={heroCover} class="w-full h-full object-cover" referrerpolicy="no-referrer" alt={heroItem.comicName} />
						</button>
					{:else}
						<button class="w-48 h-64 rounded-xl bg-surface flex items-center justify-center text-muted-foreground hover:scale-105 transition-transform" onclick={() => openComic(heroItem)}>
							<PlaceholderManga class="w-24 h-24" />
						</button>
					{/if}
					
					<div class="flex-1 space-y-4 pb-2">
						<button class="text-left hover:opacity-80 transition-opacity" onclick={() => openComic(heroItem)}>
							<h3 class="text-5xl font-black tracking-tight text-white drop-shadow-md">{heroItem.comicName}</h3>
						</button>
						<div class="flex items-center gap-2 text-lg text-white/80 font-medium">
							<BookOpen size={20} />
							<span>Capítulo {heroItem.chapterName}</span>
							<span class="opacity-50 mx-2">•</span>
							<span>Página {heroItem.lastPage + 1}</span>
						</div>
						<button 
							class="mt-4 px-8 py-3 rounded-full bg-primary text-primary-foreground font-black tracking-widest text-sm hover:opacity-90 hover:scale-105 transition-all shadow-xl shadow-primary/20"
							onclick={() => resumeReading(heroItem)}
						>
							{m['pages.history.resume'] ? m['pages.history.resume']() : 'CONTINUE READING'}
						</button>
					</div>
				</div>
			</div>

			{#if historyItems.length > 1}
				<div class="px-8">
					<h3 class="text-xl font-bold tracking-tight mb-6 opacity-80">Mais Antigos</h3>
					<div class="grid grid-cols-[repeat(auto-fill,minmax(9rem,1fr))] gap-6">
						{#each historyItems.slice(1) as item (item.comicDirectoryId)}
							{@const cover = resolveArtworkPath(item.comicCover) || undefined}
							<AcerolaCardImage
								data={{
									title: item.comicName,
									cover
								}}
								events={{
									onClick: () => openComic(item)
								}}
							>
								{#snippet footer()}
									<div class="mt-1 flex flex-col gap-1">
										<span
											class="text-overlay flex items-center gap-1 text-[10px] font-black tracking-wider uppercase"
										>
											<BookOpen size={10} />
											Capítulo {item.chapterName}
										</span>
										<span class="text-overlay text-[10px] opacity-80">
											Página {item.lastPage + 1}
										</span>
									</div>
								{/snippet}

								{#snippet placeholder()}
									<div class="h-full w-full bg-surface">
										<PlaceholderManga class="h-full w-full" />
									</div>
								{/snippet}
							</AcerolaCardImage>
						{/each}
					</div>
				</div>
			{/if}
		</div>
	{:else}
		<div class="flex flex-1 items-center justify-center p-8 text-muted-foreground">
			{m['pages.history.no_events'] ? m['pages.history.no_events']() : 'No history found.'}
		</div>
	{/if}
</div>
