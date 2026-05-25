<script lang="ts">
	import AcerolaButton from '$lib/components/acerola-button/acerola-button.svelte';
	import AcerolaButtonIcon from '$lib/components/acerola-button/acerola-button-icon.svelte';
	import AcerolaCardImage from '$lib/components/acerola-card/acerola-card-image.svelte';
	import PlaceholderManga from '$lib/assets/placeholder/placeholder_manga.svg?component';
	import ArrowLeft from '@lucide/svelte/icons/arrow-left';
	import Play from '@lucide/svelte/icons/play';
	import Bookmark from '@lucide/svelte/icons/bookmark';
	import RefreshCw from '@lucide/svelte/icons/refresh-cw';

	let {
		title,
		author,
		status,
		source,
		chaptersCount,
		description,
		cover,
		onBack
	}: {
		title: string;
		author: string;
		status: string;
		source: string;
		chaptersCount: number;
		description: string;
		cover: string | null;
		onBack: () => void;
	} = $props();
</script>

<div
	class="relative z-10 hidden h-full w-100 shrink-0 flex-col border-r border-surface/30 bg-mantle/60 backdrop-blur-3xl select-none lg:flex"
>
	<div class="flex h-full flex-col p-10">
		<AcerolaButtonIcon onclick={onBack} class="group mb-10 shadow-lg">
			<ArrowLeft size={24} class="transition-transform group-hover:-translate-x-1" />
		</AcerolaButtonIcon>

		<div
			class="scrollbar-hide flex flex-1 flex-col items-center space-y-6 overflow-y-auto text-center"
		>
			<AcerolaCardImage
				data={{
					title: title,
					cover: cover
				}}
				ui={{
					class: 'w-64 shrink-0 [&_.mt-3]:hidden'
				}}
			>
				{#snippet placeholder()}
					<div class="h-full w-full bg-surface">
						<PlaceholderManga class="h-full w-full" />
					</div>
				{/snippet}
			</AcerolaCardImage>

			<div class="w-full space-y-3 px-4">
				<div class="space-y-1">
					<h1 class="line-clamp-2 text-4xl leading-tight font-black tracking-tighter">{title}</h1>
					<p class="text-lg font-bold text-primary">{author}</p>
				</div>

				<div class="flex flex-wrap justify-center gap-2">
					<span
						class="text-text rounded-lg border border-surface/30 bg-surface/80 px-4 py-1.5 text-[10px] font-black tracking-widest uppercase shadow-sm"
						>{status}</span
					>
					<span
						class="text-text rounded-lg border border-surface/30 bg-surface/80 px-4 py-1.5 text-[10px] font-black tracking-widest uppercase shadow-sm"
						>{source}</span
					>
				</div>
			</div>

			<div class="w-full space-y-3 rounded-3xl border border-surface/30 bg-base/30 p-6 text-left">
				<div class="flex items-center justify-between">
					<h3 class="text-overlay text-[10px] font-black tracking-widest uppercase">Sinopse</h3>
					<span class="text-[9px] font-black tracking-widest text-primary/60 uppercase"
						>{chaptersCount} Caps</span
					>
				</div>
				<p class="text-subtext line-clamp-6 text-xs leading-relaxed font-medium">{description}</p>
			</div>

			<div class="mt-auto w-full space-y-3 pt-4">
				<AcerolaButton
					class="flex w-full items-center justify-center gap-3 rounded-3xl py-8 font-black"
				>
					<Play size={24} fill="currentColor" /> LER AGORA
				</AcerolaButton>

				<div class="grid grid-cols-2 gap-3">
					<AcerolaButton
						variant="outline"
						class="flex items-center justify-center gap-2 rounded-2xl py-6 text-xs font-black"
					>
						<Bookmark size={16} /> SALVAR
					</AcerolaButton>

					<AcerolaButton
						variant="outline"
						class="flex items-center justify-center gap-2 rounded-2xl py-6 text-xs font-black"
					>
						<RefreshCw size={16} /> SYNC
					</AcerolaButton>
				</div>
			</div>
		</div>
	</div>
</div>
