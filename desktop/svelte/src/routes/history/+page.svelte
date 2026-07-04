<script lang="ts">
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	import { m } from '$lib/paraglide/messages';

	import PlaceholderManga from '$lib/assets/placeholder/placeholder_manga.svg?component';
	import AcerolaButton from '$lib/components/acerola-button/acerola-button.svelte';
	import AcerolaAlertDialog from '$lib/components/acerola-alert-dialog/acerola-alert-dialog.svelte';
	import AcerolaCardImage from '$lib/components/acerola-card/acerola-card-image.svelte';
	import BookOpen from '@lucide/svelte/icons/book-open';
	import Trash2 from '@lucide/svelte/icons/trash-2';

	import { resolveArtworkPath } from '$lib/utils/artwork.utils';
	import type { ReadingHistoryDto } from '$lib/contracts/history/history.payloads';
	import { useHistory } from '$lib/hooks/store/use-history.svelte';

	const history = useHistory();

	function resumeReading(item: ReadingHistoryDto) {
		goto('/reader', {
			state: {
				comicDirectoryId: item.comicDirectoryId,
				startPage: item.lastPage,
				chapterScope: item.comicName,
				chapter: {
					id: item.chapterArchiveId,
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

	function openComic(item: ReadingHistoryDto) {
		goto(`/comic/${item.folderName}`, {
			state: {
				comicDirectoryId: item.comicDirectoryId
			}
		});
	}

	onMount(() => {
		history.fetch();
	});
</script>

<div class="flex flex-col h-full overflow-hidden">
	<div class="flex shrink-0 items-center justify-between px-8 py-6">
		<h2 class="text-2xl font-bold tracking-tight">{m['pages.history.title']()}</h2>
		{#if history.items.length > 0}
			<AcerolaAlertDialog
				data={{
					title: m['pages.history.clear_history_title'](),
					description: m['pages.history.clear_history_desc'](),
					cancelText: m['pages.history.clear_history_cancel'](),
					actionText: m['pages.history.clear_history_confirm']()
				}}
				ui={{ variant: 'destructive' }}
				events={{
					onAction: () => history.clear()
				}}
			>
				<AcerolaButton
					ui={{ variant: 'destructive', size: 'sm', class: 'gap-2 font-medium tracking-wide' }}
				>
					<Trash2 size={16} />
					{m['pages.history.clear_history']()}
				</AcerolaButton>
			</AcerolaAlertDialog>
		{/if}
	</div>

	{#if history.loading}
		<div class="flex flex-1 items-center justify-center p-8 text-muted-foreground">
			{m['pages.history.loading']()}
		</div>
	{:else if history.items.length > 0}
		{@const heroItem = history.items[0]}
		{@const heroCover = resolveArtworkPath(heroItem.comicCover)}

		<div class="flex-1 overflow-auto pb-8">
			<div class="relative w-full shrink-0 mb-8 overflow-hidden h-100 rounded-b-3xl">
				{#if heroCover}
					<img src={heroCover} class="absolute inset-0 w-full h-full object-cover" referrerpolicy="no-referrer" alt="background" />
				{:else}
					<div class="absolute inset-0 bg-linear-to-b from-primary/20 via-base/50 to-base"></div>
				{/if}
				<div class="absolute inset-0 bg-linear-to-t from-base via-base/40 to-transparent"></div>
				<div class="absolute inset-0 hidden bg-linear-to-l from-transparent via-transparent to-base/80 lg:block"></div>

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
							<span>{m['pages.history.chapter_label']({ chapter: heroItem.chapterName })}</span>
							<span class="opacity-50 mx-2">•</span>
							<span>{m['pages.history.page_label']({ page: heroItem.lastPage + 1 })}</span>
						</div>

						<AcerolaButton
							events={{ onClick: () => resumeReading(heroItem) }}
							ui={{
								class: 'mt-4 px-8 py-6 rounded-full font-black tracking-widest text-sm hover:scale-105 transition-all shadow-xl shadow-primary/20'
							}}
						>
							{m['pages.history.resume']()}
						</AcerolaButton>
					</div>
				</div>
			</div>

			{#if history.items.length > 1}
				<div class="px-8">
					<h3 class="text-xl font-bold tracking-tight mb-6 opacity-80">{m['pages.history.older']()}</h3>
					<div class="grid grid-cols-[repeat(auto-fill,minmax(9rem,1fr))] gap-6">
						{#each history.items.slice(1) as item (item.comicDirectoryId)}
							{@const cover = resolveArtworkPath(item.comicCover) || undefined}
							<AcerolaCardImage
								data={{ title: item.comicName, cover }}
								events={{ onClick: () => openComic(item) }}
							>
								{#snippet footer()}
									<div class="mt-1 flex flex-col gap-1">
										<span class="text-overlay flex items-center gap-1 text-[10px] font-black tracking-wider uppercase">
											<BookOpen size={10} />
											{m['pages.history.chapter_label']({ chapter: item.chapterName })}
										</span>
										<span class="text-overlay text-[10px] opacity-80">
											{m['pages.history.page_label']({ page: item.lastPage + 1 })}
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
			{m['pages.history.no_events']()}
		</div>
	{/if}
</div>
