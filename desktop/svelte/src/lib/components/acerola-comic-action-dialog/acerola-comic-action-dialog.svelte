<script lang="ts">
	import { fly } from 'svelte/transition';
	import { error } from '@tauri-apps/plugin-log';
	import { toast } from 'svelte-sonner';
	import EyeOff from '@lucide/svelte/icons/eye-off';
	import Trash2 from '@lucide/svelte/icons/trash-2';
	import Bookmark from '@lucide/svelte/icons/bookmark';
	import X from '@lucide/svelte/icons/x';
	import AcerolaAlertDialog from '$lib/components/acerola-alert-dialog/acerola-alert-dialog.svelte';
	import type { Category } from '$lib/contracts/bookmarks/bookmarks.payloads';

	export type ComicActionDialogProps = {
		selectedIds: number[];
		bookmarks: Category[];
		onHide: (ids: number[]) => Promise<void>;
		onDelete: (ids: number[]) => Promise<void>;
		onBookmark: (ids: number[], categoryId: number) => Promise<void>;
		onClose: () => void;
	};

	let { selectedIds, bookmarks, onHide, onDelete, onBookmark, onClose }: ComicActionDialogProps =
		$props();

	let showDeleteDialog = $state(false);
	let showBookmarkMenu = $state(false);
	let isProcessing = $state(false);

	async function handleHide() {
		if (isProcessing) return;
		isProcessing = true;
		try {
			await onHide(selectedIds);
			onClose();
		} catch (err) {
			error(`Failed to hide comics: ${err}`);
			toast.error('Failed to hide comics');
		} finally {
			isProcessing = false;
		}
	}

	async function handleDelete() {
		if (isProcessing) return;
		isProcessing = true;
		try {
			await onDelete(selectedIds);
			showDeleteDialog = false;
			onClose();
		} catch (err) {
			error(`Failed to delete comics: ${err}`);
			toast.error('Failed to delete comics');
		} finally {
			isProcessing = false;
		}
	}

	async function handleBookmark(categoryId: number) {
		if (isProcessing) return;
		isProcessing = true;
		try {
			await onBookmark(selectedIds, categoryId);
			showBookmarkMenu = false;
			onClose();
		} catch (err) {
			error(`Failed to bookmark comics: ${err}`);
			toast.error('Failed to bookmark comics');
		} finally {
			isProcessing = false;
		}
	}

	function handleDeleteClick() {
		showDeleteDialog = true;
	}
</script>

{#if selectedIds.length > 0}
	<div
		class="fixed bottom-4 left-1/2 z-50 -translate-x-1/2"
		in:fly={{ y: 20, duration: 200 }}
		out:fly={{ y: 20, duration: 200 }}
	>
		<div class="rounded-2xl bg-surface/95 shadow-2xl backdrop-blur-sm">
			<div class="flex items-center gap-2 p-3">
				<span class="px-3 text-sm font-medium text-muted-foreground">
					{selectedIds.length} selected
				</span>

				<div class="h-8 w-px bg-border"></div>

				<button
					class="hover:bg-surface-hover flex items-center gap-2 rounded-xl px-3 py-2 text-sm transition-colors"
					onclick={handleHide}
					disabled={isProcessing}
				>
					<EyeOff size={16} />
					<span>Hide</span>
				</button>

				<button
					class="flex items-center gap-2 rounded-xl px-3 py-2 text-sm text-destructive transition-colors hover:bg-destructive/10"
					onclick={handleDeleteClick}
					disabled={isProcessing}
				>
					<Trash2 size={16} />
					<span>Delete</span>
				</button>

				<div class="relative">
					<button
						class="hover:bg-surface-hover flex items-center gap-2 rounded-xl px-3 py-2 text-sm transition-colors"
						onclick={() => (showBookmarkMenu = !showBookmarkMenu)}
						disabled={isProcessing}
					>
						<Bookmark size={16} />
						<span>Bookmark</span>
					</button>

					{#if showBookmarkMenu}
						<div
							class="absolute bottom-full left-0 mb-2 min-w-48 rounded-xl bg-surface shadow-lg"
							in:fly={{ y: 10, duration: 150 }}
						>
							<div class="p-2">
								{#each bookmarks as category}
									<button
										class="hover:bg-surface-hover flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm transition-colors"
										onclick={() => handleBookmark(category.id)}
									>
										<span
											class="h-3 w-3 rounded-full"
											style="background-color: #{category.color.toString(16).padStart(6, '0')}"
										></span>
										<span>{category.name}</span>
									</button>
								{/each}
							</div>
						</div>
					{/if}
				</div>

				<button
					class="hover:bg-surface-hover ml-2 rounded-full p-2 transition-colors"
					onclick={onClose}
				>
					<X size={16} />
				</button>
			</div>
		</div>
	</div>
{/if}

<AcerolaAlertDialog
	state={{ open: showDeleteDialog }}
	data={{
		title: 'Delete Comics',
		description:
			'Are you sure you want to delete the selected comics? This will only remove them from the database, the files will remain unchanged.',
		cancelText: 'Cancel',
		actionText: 'Delete'
	}}
	events={{
		onAction: handleDelete,
		onCancel: () => (showDeleteDialog = false)
	}}
	ui={{ variant: 'destructive' }}
/>
