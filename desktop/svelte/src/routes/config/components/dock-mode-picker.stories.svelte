<script lang="ts" module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import DockModePicker from './dock-mode-picker.svelte';

	const { Story } = defineMeta({
		title: 'Pages/Config/DockModePicker',
		component: DockModePicker,
		tags: ['autodocs'],
		parameters: {
			docs: {
				description: {
					component: 'Seletor do modo de comportamento da dock de navegação (fixa ou com gesto de hover).'
				}
			}
		},
		argTypes: {
			data: { description: 'Modo atual da dock', control: 'object' },
			events: {
				description: 'Callback quando um modo é selecionado',
				control: 'object'
			}
		}
	});
</script>

<script lang="ts">
	import type { NavDockMode } from '$lib/hooks/preferences/use-nav-dock-mode.svelte';

	let mode = $state<NavDockMode>('fixed');

	function onSelect(next: NavDockMode) {
		mode = next;
	}
</script>

<Story name="Default" asChild>
	<DockModePicker data={{ mode }} events={{ onSelect }} />
</Story>
