<script lang="ts" module>
	import AcerolaModePicker from '$lib/components/acerola-mode-picker/acerola-mode-picker.svelte';
	import SidebarProvider from '$lib/components/ui/sidebar/sidebar-provider.svelte';
	import HistoryIcon from '@lucide/svelte/icons/history';
	import LibraryIcon from '@lucide/svelte/icons/library';
	import SettingsIcon from '@lucide/svelte/icons/settings';
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import type { SidebarItem } from './acerola-sidebar.svelte';
	import AcerolasSidebar from './acerola-sidebar.svelte';

	const items: SidebarItem[] = [
		{ href: '/home', label: 'Biblioteca', icon: LibraryIcon },
		{ href: '/history', label: 'Histórico', icon: HistoryIcon },
		{ href: '/config', label: 'Configurações', icon: SettingsIcon }
	];

	const { Story } = defineMeta({
		title: 'Components/AcerolasSidebar',
		component: AcerolasSidebar,
		tags: ['autodocs'],
		parameters: {
			layout: 'fullscreen',
			docs: {
				description: {
					component: 'Sidebar de navegação principal da aplicação.'
				}
			}
		},
		argTypes: {
			data: { description: 'Dados da sidebar', control: 'object' },
			ui: { description: 'Configuração visual da sidebar', control: 'object' },
			footer: {
				description: 'Snippet opcional para o rodapé da sidebar',
				control: 'object'
			}
		}
	});
</script>

<Story name="Default" asChild>
	<SidebarProvider>
		<AcerolasSidebar data={{ items }} />
	</SidebarProvider>
</Story>

<Story name="With Footer" asChild>
	<SidebarProvider>
		<AcerolasSidebar data={{ items }}>
			{#snippet footer()}
				<AcerolaModePicker />
			{/snippet}
		</AcerolasSidebar>
	</SidebarProvider>
</Story>
