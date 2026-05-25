<script lang="ts">
	import BookOpen from '@lucide/svelte/icons/book-open';
	import Star from '@lucide/svelte/icons/star';
	import List from '@lucide/svelte/icons/list';
	import { ScrollArea } from '$lib/components/ui/scroll-area/index.js';

	let {
		banner,
		genres = []
	}: {
		banner?: string | null;
		genres?: string[];
	} = $props();
</script>

<div class="relative h-75 w-full shrink-0 lg:h-112.5">
	{#if banner}
		<div class="relative h-full w-full">
			<img
				src={banner}
				alt="Banner"
				class="h-full w-full object-cover"
				referrerpolicy="no-referrer"
			/>
			<div class="absolute inset-0 bg-linear-to-t from-base via-base/20 to-transparent"></div>
			<div
				class="absolute inset-0 hidden bg-linear-to-l from-transparent via-transparent to-base/40 lg:block"
			></div>
		</div>
	{:else}
		<div class="h-full w-full bg-linear-to-b from-primary/20 via-base/50 to-base"></div>
	{/if}

	<!-- Floating stats and Genres on Banner -->
	<div class="absolute right-6 bottom-6 left-6 flex flex-col gap-4 lg:left-16">
		<div class="flex shrink-0 gap-4 lg:gap-6">
			<div
				class="flex items-center gap-3 rounded-2xl border border-surface/30 bg-surface/20 px-6 py-3 backdrop-blur-xl"
			>
				<Star size={20} class="fill-yellow-400 text-yellow-400" />
				<span class="text-xl font-black tracking-tighter">9.8</span>
			</div>

			<div
				class="flex items-center gap-3 rounded-2xl border border-surface/30 bg-surface/20 px-6 py-3 backdrop-blur-xl"
			>
				<List size={20} class="text-primary" />
				<span class="text-xl font-black tracking-tighter">Popular</span>
			</div>
		</div>

		{#if genres.length > 0}
			<ScrollArea
				orientation="horizontal"
				class="w-full whitespace-nowrap"
				scrollbarXClasses="h-1.5 opacity-0 group-hover:opacity-100 transition-opacity"
			>
				<div class="flex gap-2 pr-6 pb-3">
					{#each genres as genre}
						<span
							class="text-text rounded-full border border-surface/30 bg-surface/40 px-4 py-1.5 text-[10px] font-black tracking-widest whitespace-nowrap uppercase shadow-sm backdrop-blur-xl"
							>{genre}</span
						>
					{/each}
				</div>
			</ScrollArea>
		{/if}
	</div>
</div>
