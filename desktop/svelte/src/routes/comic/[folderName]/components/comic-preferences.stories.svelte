<script lang="ts" module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import ComicPreferences from './comic-preferences.svelte';

	const { Story } = defineMeta({
		title: 'Pages/Comic/ComicPreferences',
		component: ComicPreferences,
		tags: ['autodocs'],
		parameters: {
			docs: {
				description: {
					component: 'Painel de preferências da página do comic.'
				}
			}
		}
	});
</script>

<script lang="ts">
	let displayMode = $state('Lista');
	let chaptersPerPage = $state('100');
	let mediaType = $state('Manga');
	let volumeViewMode = $state<'cover' | 'banner'>('cover');
</script>

<Story name="Default" asChild>
	<ComicPreferences
		data={{ hasVolumeStructure: true }}
		state={{
			chaptersPerPage,
			volumeViewMode,
			bookmarkId: null
		}}
		events={{
			onChaptersPerPageChange: (value) => (chaptersPerPage = value),
			onVolumeViewModeChange: (value) => (volumeViewMode = value),
			onBookmarkChange: () => {}
		}}
	/>
</Story>
