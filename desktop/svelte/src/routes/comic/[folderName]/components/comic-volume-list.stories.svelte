<script lang="ts" module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import ComicVolumeList, { type Volume } from './comic-volume-list.svelte';

	const { Story } = defineMeta({
		title: 'Pages/Comic/ComicVolumeList',
		component: ComicVolumeList,
		tags: ['autodocs'],
		parameters: {
			docs: {
				description: {
					component: 'Lista expansível de volumes do comic.'
				}
			}
		}
	});
</script>

<script lang="ts">
	const volumes: Volume[] = [
		{
			id: 'v1',
			title: 'Volume 1',
			chapters: [
				{ id: 'c1', title: 'Capítulo 1: O Início', date: '12 Out 2023', isRead: true },
				{ id: 'c2', title: 'Capítulo 2: A Descoberta', date: '19 Out 2023', isRead: false }
			]
		},
		{
			id: 'v2',
			title: 'Volume 2',
			chapters: [{ id: 'c3', title: 'Capítulo 3: O Desafio', date: '26 Out 2023', isRead: false }]
		}
	];
</script>

<Story name="With Volumes" asChild>
	<ComicVolumeList {volumes} />
</Story>

<Story name="Empty" asChild>
	<ComicVolumeList volumes={[]} />
</Story>
