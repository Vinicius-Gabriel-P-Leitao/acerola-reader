<script module lang="ts">
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import AcerolaAlertDialog from './acerola-alert-dialog.svelte';
	import AcerolaButton from '../acerola-button/acerola-button.svelte';

	const { Story } = defineMeta({
		component: AcerolaAlertDialog,
		title: 'Components/AcerolaAlertDialog',
		argTypes: {
			'data.title': { control: 'text' },
			'data.description': { control: 'text' },
			'data.cancelText': { control: 'text' },
			'data.actionText': { control: 'text' },
			'ui.variant': {
				control: 'select',
				options: ['default', 'destructive', 'outline', 'secondary', 'ghost', 'link']
			}
		}
	});
</script>

<Story
	name="Default"
	args={{
		state: { open: false },
		data: {
			title: 'Are you absolutely sure?',
			description: 'This action cannot be undone. This will permanently delete your account and remove your data from our servers.',
			cancelText: 'Cancel',
			actionText: 'Continue'
		},
		ui: {
			variant: 'default'
		}
	}}
>
	{#snippet children(args)}
		<AcerolaAlertDialog {...args}>
			<AcerolaButton>Open Dialog</AcerolaButton>
		</AcerolaAlertDialog>
	{/snippet}
</Story>

<Story
	name="Destructive"
	args={{
		state: { open: false },
		data: {
			title: 'Delete everything?',
			description: 'This will permanently delete all your files. Are you sure?',
			cancelText: 'Cancel',
			actionText: 'Delete'
		},
		ui: {
			variant: 'destructive'
		}
	}}
>
	{#snippet children(args)}
		<AcerolaAlertDialog {...args}>
			<AcerolaButton ui={{ variant: 'destructive' }}>Delete All</AcerolaButton>
		</AcerolaAlertDialog>
	{/snippet}
</Story>
