<script module lang="ts">
	import { defineMeta } from '@storybook/addon-svelte-csf';
	import AcerolaComicActionDialog from './acerola-comic-action-dialog.svelte';
	import type { Category } from '$lib/contracts/bookmarks/bookmarks.payloads';

	const mockBookmarks: Category[] = [
		{ id: 1, name: 'Favoritos', color: 0xff0000 },
		{ id: 2, name: 'Lendo', color: 0x00ff00 },
		{ id: 3, name: 'Completados', color: 0x0000ff }
	];

	const { Story } = defineMeta({
		component: AcerolaComicActionDialog,
		title: 'Components/AcerolaComicActionDialog'
	});
</script>

<Story
	name="Default"
	args={{
		selectedIds: [1, 2, 3],
		bookmarks: mockBookmarks
	}}
>
	{#snippet children()}
		<div class="min-h-[400px] bg-surface p-8">
			<AcerolaComicActionDialog
				selectedIds={[1, 2, 3]}
				bookmarks={mockBookmarks}
				onHide={async () => {}}
				onDelete={async () => {}}
				onBookmark={async () => {}}
				onClose={() => {}}
			/>
		</div>
	{/snippet}
</Story>

<Story
	name="Single Selection"
	args={{
		selectedIds: [1],
		bookmarks: mockBookmarks
	}}
>
	{#snippet children()}
		<div class="min-h-[400px] bg-surface p-8">
			<AcerolaComicActionDialog
				selectedIds={[1]}
				bookmarks={mockBookmarks}
				onHide={async () => {}}
				onDelete={async () => {}}
				onBookmark={async () => {}}
				onClose={() => {}}
			/>
		</div>
	{/snippet}
</Story>

<Story
	name="Many Selected"
	args={{
		selectedIds: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
		bookmarks: mockBookmarks
	}}
>
	{#snippet children()}
		<div class="min-h-[400px] bg-surface p-8">
			<AcerolaComicActionDialog
				selectedIds={[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]}
				bookmarks={mockBookmarks}
				onHide={async () => {}}
				onDelete={async () => {}}
				onBookmark={async () => {}}
				onClose={() => {}}
			/>
		</div>
	{/snippet}
</Story>

<Story
	name="Empty"
	args={{
		selectedIds: [],
		bookmarks: mockBookmarks
	}}
>
	{#snippet children()}
		<div class="min-h-[400px] bg-surface p-8">
			<AcerolaComicActionDialog
				selectedIds={[]}
				bookmarks={mockBookmarks}
				onHide={async () => {}}
				onDelete={async () => {}}
				onBookmark={async () => {}}
				onClose={() => {}}
			/>
		</div>
	{/snippet}
</Story>

<Story
	name="No Bookmarks"
	args={{
		selectedIds: [1, 2],
		bookmarks: []
	}}
>
	{#snippet children()}
		<div class="min-h-[400px] bg-surface p-8">
			<AcerolaComicActionDialog
				selectedIds={[1, 2]}
				bookmarks={[]}
				onHide={async () => {}}
				onDelete={async () => {}}
				onBookmark={async () => {}}
				onClose={() => {}}
			/>
		</div>
	{/snippet}
</Story>
