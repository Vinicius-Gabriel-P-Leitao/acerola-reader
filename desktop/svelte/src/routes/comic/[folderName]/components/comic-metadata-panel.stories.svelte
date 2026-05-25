<script lang="ts" module>
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import ComicMetadataPanel from './comic-metadata-panel.svelte';

	const { Story } = defineMeta({
		title: 'Pages/Comic/ComicMetadataPanel',
		component: ComicMetadataPanel,
		tags: ['autodocs'],
		parameters: {
			docs: {
				description: {
					component:
						'Painel lateral de metadados do comic, exibindo capa, título, autor, sinopse e ações.'
				}
			}
		}
	});
</script>

<script lang="ts">
	const title = 'Manga Title';
	const author = 'Autor Desconhecido';
	const category = 'Manga';
	const chaptersCount = 10;
	const description = 'Uma jornada épica espera por você. Sinopse muito longa para testar o clamp.';
	const cover = 'https://placehold.co/400x600/2a2a35/a6accd?text=Cover';
	const onBack = () => console.log('Back clicked');
</script>

<Story name="Default" asChild>
	<ComicMetadataPanel {title} {author} {category} {chaptersCount} {description} {cover} {onBack} />
</Story>
