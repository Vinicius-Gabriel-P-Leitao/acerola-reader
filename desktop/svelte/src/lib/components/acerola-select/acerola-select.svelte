<script module lang="ts">
	export type AcerolaSelectOption = {
		value: string;
		label: string;
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

	let selectedLabel = $derived(
		data.options.find((it: AcerolaSelectOption) => it.value === value)?.label ?? placeholder
	);

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
		{selectedLabel}
	</SelectTrigger>

	<SelectContent>
		{#each data.options as option (option.value)}
			<SelectItem value={option.value} label={option.label}>
				{option.label}
			</SelectItem>
		{/each}
	</SelectContent>
</Select>
