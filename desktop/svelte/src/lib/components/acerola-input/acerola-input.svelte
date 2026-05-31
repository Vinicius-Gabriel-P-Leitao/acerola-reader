<script lang="ts">
	import { Input, type InputProps } from '$lib/components/ui/input';
	import { cn } from '$lib/utils/cn.utils';

	let { value = $bindable(), class: className, ...props }: InputProps = $props();
</script>

<Input
	bind:value
	class={cn('w-auto min-w-48 text-foreground disabled:pointer-events-auto', className)}
	{...props}
/>
