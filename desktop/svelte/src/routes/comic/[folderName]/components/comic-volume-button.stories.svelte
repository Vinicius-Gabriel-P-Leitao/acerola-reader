<script module lang="ts">
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import ComicVolumeButton from './comic-volume-button.svelte';

	export const meta = defineMeta({
		title: 'Components/Comic/ComicVolumeButton',
		component: ComicVolumeButton,
		argTypes: {
			title: { control: 'text' },
			totalChapters: { control: 'number' },
			viewMode: { control: 'radio', options: ['cover', 'banner'] },
			isExpanded: { control: 'boolean' }
		}
	});
</script>

<script lang="ts">
	import { Story } from '@storybook/addon-svelte-csf';
</script>

<Story
	name="Default (No Image)"
	args={{
		title: 'Volume 1',
		totalChapters: 12,
		viewMode: 'cover',
		isExpanded: false
	}}
/>

<Story
	name="With Cover"
	args={{
		title: 'Volume 1',
		totalChapters: 12,
		viewMode: 'cover',
		coverUri: 'https://placehold.co/400x600/png',
		isExpanded: false
	}}
/>

<Story
	name="With Banner"
	args={{
		title: 'Volume 1',
		totalChapters: 12,
		viewMode: 'banner',
		bannerUri: 'https://placehold.co/800x400/png',
		isExpanded: false
	}}
/>

<Story
	name="Expanded"
	args={{
		title: 'Volume 1',
		totalChapters: 12,
		viewMode: 'cover',
		isExpanded: true
	}}
/>
