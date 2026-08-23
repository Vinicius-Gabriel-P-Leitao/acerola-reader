<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import { Button } from '$lib/components/ui/button';
	import FileTextIcon from '@lucide/svelte/icons/file-text';
	import FileArchiveIcon from '@lucide/svelte/icons/file-archive';
	import LayersIcon from '@lucide/svelte/icons/layers';
	import SparklesIcon from '@lucide/svelte/icons/sparkles';
	import CheckCircle2Icon from '@lucide/svelte/icons/check-circle-2';

	let { onNext, onPrev } = $props<{ onNext: () => void; onPrev: () => void }>();

	const formats = $derived([
		{
			extension: 'CBZ',
			name: m['onboarding.formats.cbz.title'](),
			badge: m['onboarding.formats.cbz.badge'](),
			badgeClass: 'bg-primary/10 text-primary border-primary/30',
			cardClass: 'border-primary/40 bg-gradient-to-b from-primary/5 to-card/50 shadow-md',
			icon: FileArchiveIcon,
			iconColor: 'text-primary bg-primary/10',
			description: m['onboarding.formats.cbz.desc'](),
			features: [m['onboarding.formats.cbz.feature_1'](), m['onboarding.formats.cbz.feature_2']()]
		},
		{
			extension: 'CBR',
			name: m['onboarding.formats.cbr.title'](),
			badge: m['onboarding.formats.cbr.badge'](),
			badgeClass: 'bg-chart-3/10 text-chart-3 border-chart-3/30',
			cardClass: 'border-border/40 bg-card/50 hover:border-chart-3/40 transition-all',
			icon: LayersIcon,
			iconColor: 'text-chart-3 bg-chart-3/10',
			description: m['onboarding.formats.cbr.desc'](),
			features: [m['onboarding.formats.cbr.feature_1'](), m['onboarding.formats.cbr.feature_2']()]
		},
		{
			extension: 'PDF',
			name: m['onboarding.formats.pdf.title'](),
			badge: m['onboarding.formats.pdf.badge'](),
			badgeClass: 'bg-chart-1/10 text-chart-1 border-chart-1/30',
			cardClass: 'border-border/40 bg-card/50 hover:border-chart-1/40 transition-all',
			icon: FileTextIcon,
			iconColor: 'text-chart-1 bg-chart-1/10',
			description: m['onboarding.formats.pdf.desc'](),
			features: [m['onboarding.formats.pdf.feature_1'](), m['onboarding.formats.pdf.feature_2']()]
		}
	]);
</script>

<div class="flex h-full w-full items-center justify-center overflow-y-auto p-6 sm:p-8">
	<div class="w-full max-w-3xl space-y-8 animate-in fade-in-50 duration-300">
		<!-- Header -->
		<div class="text-center space-y-2">
			<div class="inline-flex size-14 items-center justify-center rounded-2xl bg-chart-5/10 text-chart-5 shadow-inner mb-2">
				<FileArchiveIcon size={30} />
			</div>
			<h1 class="text-3xl font-bold tracking-tight text-foreground">{m['onboarding.formats.title']()}</h1>
			<p class="text-sm text-muted-foreground max-w-md mx-auto">{m['onboarding.formats.desc']()}</p>
		</div>

		<!-- Grid Formatos -->
		<div class="grid grid-cols-1 md:grid-cols-3 gap-4">
			{#each formats as item}
				<div class="group relative flex flex-col justify-between rounded-2xl border p-5 backdrop-blur-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-xl {item.cardClass}">
					<div>
						<!-- Top row: Icon & Badge -->
						<div class="flex items-center justify-between mb-4">
							<div class="flex size-11 items-center justify-center rounded-xl p-2.5 {item.iconColor}">
								<item.icon size={22} />
							</div>
							<span class="rounded-full border px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider {item.badgeClass}">
								{item.badge}
							</span>
						</div>

						<!-- Title & Extension -->
						<h3 class="text-2xl font-black tracking-tight text-foreground mb-0.5">{item.extension}</h3>
						<p class="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">{item.name}</p>

						<!-- Description -->
						<p class="text-xs leading-relaxed text-muted-foreground/90">{item.description}</p>
					</div>

					<!-- Features list -->
					<div class="mt-4 pt-3 border-t border-border/20 space-y-1.5">
						{#each item.features as feat}
							<div class="flex items-center gap-1.5 text-[11px] text-muted-foreground">
								<CheckCircle2Icon size={12} class="text-primary shrink-0" />
								<span>{feat}</span>
							</div>
						{/each}
					</div>
				</div>
			{/each}
		</div>

		<!-- Bottom callout -->
		<div class="flex items-center gap-3 rounded-2xl border border-border/40 bg-surface/40 p-4 text-xs text-muted-foreground backdrop-blur-md">
			<div class="flex size-8 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
				<SparklesIcon size={18} />
			</div>
			<p class="leading-normal">
				<strong class="font-semibold text-foreground">{m['onboarding.formats.sync_note.title']()}</strong> {m['onboarding.formats.sync_note.content']()}
			</p>
		</div>

		<!-- Actions -->
		<div class="flex items-center justify-between pt-2">
			<Button onclick={onPrev} variant="outline" class="rounded-xl px-6">
				{m['onboarding.back']()}
			</Button>
			<Button onclick={onNext} class="rounded-xl px-8 shadow-md hover:shadow-lg transition-all font-semibold">
				{m['onboarding.next']()}
			</Button>
		</div>
	</div>
</div>
