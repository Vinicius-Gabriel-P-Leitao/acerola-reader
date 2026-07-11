<script lang="ts">
	import { m } from '$lib/paraglide/messages';
	import { Button } from '$lib/components/ui/button';
	import FileTextIcon from '@lucide/svelte/icons/file-text';

	let { onNext, onPrev } = $props<{ onNext: () => void; onPrev: () => void }>();
</script>

<div class="flex h-full w-full items-center justify-center p-8">
	<div class="w-full max-w-xl space-y-8">
		<div class="flex items-center justify-center gap-3">
			<FileTextIcon class="text-chart-5" size={32} />
			<h1 class="text-3xl font-bold text-foreground">{m['onboarding.formats.title']()}</h1>
		</div>

		<div class="rounded-2xl border border-border/40 bg-card/50 p-6 backdrop-blur-sm">
			<p class="mb-4 text-muted-foreground">{m['onboarding.formats.desc']()}</p>

			<div class="grid grid-cols-1 gap-4">
				<div class="rounded-xl border border-border/40 bg-background/50 p-4">
					<h3 class="font-semibold text-foreground">CBZ</h3>
				</div>
				<div class="rounded-xl border border-border/40 bg-background/50 p-4">
					<h3 class="font-semibold text-foreground">CBR</h3>
				</div>
				<div class="rounded-xl border border-border/40 bg-background/50 p-4">
					<h3 class="mb-2 font-semibold text-foreground">PDF</h3>
					<p class="text-sm text-muted-foreground">{m['onboarding.formats.pdf']()}</p>
				</div>
			</div>
		</div>

		<div class="flex justify-between">
			<Button onclick={onPrev} variant="outline" class="rounded-xl">
				{m['onboarding.back']()}
			</Button>
			<Button onclick={onNext} class="rounded-xl">
				{m['onboarding.next']()}
			</Button>
		</div>
	</div>
</div>
