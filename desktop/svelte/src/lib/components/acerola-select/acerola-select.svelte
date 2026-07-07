<script module lang="ts">
	export type AcerolaSelectOption = {
		value: string;
		label: string;
		color?: number;
	};

	export type AcerolaSelectProps = {
		data: {
			options: AcerolaSelectOption[];
		};
		state?: {
			value?: string;
		};
		events?: {
			onValueChange?: (value: string) => void;
		};
		ui?: {
			class?: string;
			placeholder?: string;
		};
	};
</script>

<script lang="ts">
	import { Select, SelectContent, SelectItem, SelectTrigger } from '$lib/components/ui/select';
	import { m } from '$lib/paraglide/messages';
	import { cn } from '$lib/utils/cn.utils';

	let { data, state: control, events, ui }: AcerolaSelectProps = $props();

	let value = $state('');
	let lastValue = $state('');

	const placeholder = $derived(ui?.placeholder ?? m['components.select.placeholder']());

	let selectedOption = $derived(data.options.find((it) => it.value === value));
	let selectedLabel = $derived(selectedOption?.label ?? placeholder);

	$effect(() => {
		if (control?.value === undefined) return;

		const nextValue = control.value;

		if (nextValue !== lastValue) {
			value = nextValue;
			lastValue = nextValue;
		}
	});

	$effect(() => {
		if (value === lastValue) return;

		lastValue = value;
		events?.onValueChange?.(value);
	});
</script>

<Select type="single" bind:value>
	<SelectTrigger class={cn('w-auto min-w-48 justify-between', ui?.class)}>
		<div class="flex items-center gap-2">
			{#if selectedOption?.color != null}
				<div 
					class="h-3 w-3 rounded-full" 
					style="background-color: #{((selectedOption.color & 0xFFFFFF).toString(16).padStart(6, '0'))}"
				></div>
			{/if}
			<span>{selectedLabel}</span>
		</div>
	</SelectTrigger>

	<SelectContent>
		{#each data.options as option (option.value)}
			<SelectItem value={option.value} label={option.label}>
				<div class="flex items-center gap-2">
					{#if option.color != null}
						<div 
							class="h-3 w-3 rounded-full" 
							style="background-color: #{((option.color & 0xFFFFFF).toString(16).padStart(6, '0'))}"
						></div>
					{/if}
					<span>{option.label}</span>
				</div>
			</SelectItem>
		{/each}
	</SelectContent>
</Select>
