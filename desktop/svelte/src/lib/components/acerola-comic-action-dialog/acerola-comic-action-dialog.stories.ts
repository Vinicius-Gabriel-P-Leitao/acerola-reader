import type { Category } from '$lib/contracts/bookmarks/bookmarks.payloads';

const mockBookmarks: Category[] = [
	{ id: 1, name: 'Favoritos', color: 0xff0000 },
	{ id: 2, name: 'Lendo', color: 0x00ff00 },
	{ id: 3, name: 'Completados', color: 0x0000ff }
];

export default {
	title: 'Components/AcerolaComicActionDialog',
	component: 'AcerolaComicActionDialog',
	tags: ['autodocs']
};

export const Default = {
	render() {
		return {
			template: `
				<div class="min-h-[400px] bg-surface p-8">
					<div class="text-sm text-muted-foreground mb-4">
						Abra o Canvas para ver o dialog na parte inferior
					</div>
					<script>
						import AcerolaComicActionDialog from './acerola-comic-action-dialog.svelte';
						
						const bookmarks = ${JSON.stringify(mockBookmarks)};
						const selectedIds = [1, 2, 3];
						
						async function handleHide(ids) {
							console.log('Hide:', ids);
						}
						
						async function handleDelete(ids) {
							console.log('Delete:', ids);
						}
						
						async function handleBookmark(ids, categoryId) {
							console.log('Bookmark:', ids, 'Category:', categoryId);
						}
						
						function handleClose() {
							console.log('Close');
						}
					</script>
					
					<AcerolaComicActionDialog
						selectedIds={selectedIds}
						bookmarks={bookmarks}
						onHide={handleHide}
						onDelete={handleDelete}
						onBookmark={handleBookmark}
						onClose={handleClose}
					/>
				</div>
			`
		};
	}
};

export const SingleSelection = {
	render() {
		return {
			template: `
				<div class="min-h-[400px] bg-surface p-8">
					<script>
						import AcerolaComicActionDialog from './acerola-comic-action-dialog.svelte';
						
						const bookmarks = ${JSON.stringify(mockBookmarks)};
						const selectedIds = [1];
					</script>
					
					<AcerolaComicActionDialog
						selectedIds={selectedIds}
						bookmarks={bookmarks}
						onHide={() => {}}
						onDelete={() => {}}
						onBookmark={() => {}}
						onClose={() => {}}
					/>
				</div>
			`
		};
	}
};

export const ManySelected = {
	render() {
		return {
			template: `
				<div class="min-h-[400px] bg-surface p-8">
					<script>
						import AcerolaComicActionDialog from './acerola-comic-action-dialog.svelte';
						
						const bookmarks = ${JSON.stringify(mockBookmarks)};
						const selectedIds = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
					</script>
					
					<AcerolaComicActionDialog
						selectedIds={selectedIds}
						bookmarks={bookmarks}
						onHide={() => {}}
						onDelete={() => {}}
						onBookmark={() => {}}
						onClose={() => {}}
					/>
				</div>
			`
		};
	}
};

export const Empty = {
	render() {
		return {
			template: `
				<div class="min-h-[400px] bg-surface p-8">
					<div class="text-sm text-muted-foreground">
						Nenhum item selecionado - dialog não deve aparecer
					</div>
					<script>
						import AcerolaComicActionDialog from './acerola-comic-action-dialog.svelte';
						
						const bookmarks = ${JSON.stringify(mockBookmarks)};
						const selectedIds = [];
					</script>
					
					<AcerolaComicActionDialog
						selectedIds={selectedIds}
						bookmarks={bookmarks}
						onHide={() => {}}
						onDelete={() => {}}
						onBookmark={() => {}}
						onClose={() => {}}
					/>
				</div>
			`
		};
	}
};

export const NoBookmarks = {
	render() {
		return {
			template: `
				<div class="min-h-[400px] bg-surface p-8">
					<script>
						import AcerolaComicActionDialog from './acerola-comic-action-dialog.svelte';
						
						const bookmarks = [];
						const selectedIds = [1, 2];
					</script>
					
					<AcerolaComicActionDialog
						selectedIds={selectedIds}
						bookmarks={bookmarks}
						onHide={() => {}}
						onDelete={() => {}}
						onBookmark={() => {}}
						onClose={() => {}}
					/>
				</div>
			`
		};
	}
};
