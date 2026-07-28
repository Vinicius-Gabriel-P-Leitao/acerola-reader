<script lang="ts">
	import EyeOff from '@lucide/svelte/icons/eye-off';
	import Trash2 from '@lucide/svelte/icons/trash-2';
	import Bookmark from '@lucide/svelte/icons/bookmark';
	import CheckSquare from '@lucide/svelte/icons/check-square';
	import ChevronDown from '@lucide/svelte/icons/chevron-down';
	import ChevronUp from '@lucide/svelte/icons/chevron-up';
	import { error } from '@tauri-apps/plugin-log';
	import { toast } from 'svelte-sonner';
	import AcerolaDialog from '$lib/components/acerola-dialog/acerola-dialog.svelte';
	import AcerolaAlertDialog from '$lib/components/acerola-alert-dialog/acerola-alert-dialog.svelte';
	import AcerolaButton from '$lib/components/acerola-button/acerola-button.svelte';
	import { m } from '$lib/paraglide/messages';
	import type { Category } from '$lib/contracts/bookmarks/bookmarks.payloads';

	export type ComicActionDialogProps = {
		open?: boolean;
		selectedIds: number[];
		totalCount?: number;
		bookmarks: Category[];
		onHide: (ids: number[]) => Promise<void>;
		onDelete: (ids: number[]) => Promise<void>;
		onBookmark: (ids: number[], categoryId: number) => Promise<void>;
		onSelectAll?: () => void;
		onClose: () => void;
	};

	let {
		open = false,
		selectedIds,
		totalCount = 0,
		bookmarks,
		onHide,
		onDelete,
		onBookmark,
		onSelectAll,
		onClose
	}: ComicActionDialogProps = $props();

	let showHideDialog = $state(false);
	let showDeleteDialog = $state(false);
	let showBookmarkMenu = $state(false);
	let isProcessing = $state(false);

	const isAllSelected = $derived(totalCount > 0 && selectedIds.length === totalCount);

	async function handleHide() {
		if (isProcessing) return;
		isProcessing = true;
		try {
			await onHide(selectedIds);
			showHideDialog = false;
			onClose();
		} catch (err: unknown) {
			const msg = typeof err === 'object' && err !== null ? JSON.stringify(err) : String(err);
			error(`Failed to hide comics: ${msg}`);
			toast.error(m['pages.home.toast.hide_error']());
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
		} catch (err: unknown) {
			const msg = typeof err === 'object' && err !== null ? JSON.stringify(err) : String(err);
			error(`Failed to delete comics: ${msg}`);
			toast.error(m['pages.home.toast.delete_error']());
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
		} catch (err: unknown) {
			const msg = typeof err === 'object' && err !== null ? JSON.stringify(err) : String(err);
			error(`Failed to bookmark comics: ${msg}`);
			toast.error(m['pages.home.toast.bookmark_error']());
		} finally {
			isProcessing = false;
		}
	}

	function handleHideClick() {
		showHideDialog = true;
	}

	function handleDeleteClick() {
		showDeleteDialog = true;
	}
</script>

<AcerolaDialog
	state={{ open }}
	data={{
		title: m['pages.home.actions.title'](),
		description: m['pages.home.actions.description']({ count: selectedIds.length })
	}}
	events={{
		onOpenChange: (isOpen) => {
			if (!isOpen) onClose();
		}
	}}
>
	<div class="flex flex-col gap-3 py-2">
		<!-- Select All Option inside Dialog -->
		{#if onSelectAll && totalCount > 0}
			<AcerolaButton
				ui={{ variant: 'outline', class: 'w-full justify-start rounded-xl font-medium', disabled: isProcessing }}
				events={{ onClick: onSelectAll }}
			>
				<CheckSquare size={18} class="mr-2" />
				<span>
					{isAllSelected
						? m['pages.home.selection.deselect_all']()
						: m['pages.home.selection.select_all_total']({ total: totalCount })}
				</span>
			</AcerolaButton>
		{/if}

		<!-- Bookmark Section -->
		<div class="rounded-xl border border-border bg-surface/50 p-2">
			<AcerolaButton
				ui={{ variant: 'ghost', class: 'w-full justify-between rounded-lg', disabled: isProcessing || selectedIds.length === 0 }}
				events={{ onClick: () => (showBookmarkMenu = !showBookmarkMenu) }}
			>
				<div class="flex items-center gap-2">
					<Bookmark size={18} />
					<span>{m['pages.home.actions.bookmark']()}</span>
				</div>
				{#if showBookmarkMenu}
					<ChevronUp size={16} />
				{:else}
					<ChevronDown size={16} />
				{/if}
			</AcerolaButton>

			{#if showBookmarkMenu}
				<div class="mt-2 flex flex-col gap-1 border-t border-border pt-2">
					{#if bookmarks.length === 0}
						<span class="px-3 py-1 text-xs text-muted-foreground">
							No bookmark categories available
						</span>
					{:else}
						{#each bookmarks as category}
							<AcerolaButton
								ui={{ variant: 'ghost', class: 'w-full justify-start rounded-lg text-sm', disabled: isProcessing || selectedIds.length === 0 }}
								events={{ onClick: () => handleBookmark(category.id) }}
							>
								<span
									class="mr-2 h-3 w-3 rounded-full"
									style="background-color: #{category.color.toString(16).padStart(6, '0')}"
								></span>
								<span>{category.name}</span>
							</AcerolaButton>
						{/each}
					{/if}
				</div>
			{/if}
		</div>

		<!-- Hide Action (Destructive Confirmation Alert) -->
		<AcerolaButton
			ui={{
				variant: 'outline',
				class: 'w-full justify-start rounded-xl text-amber-500 hover:text-amber-600 hover:bg-amber-500/10 border-amber-500/30',
				disabled: isProcessing || selectedIds.length === 0
			}}
			events={{ onClick: handleHideClick }}
		>
			<EyeOff size={18} class="mr-2" />
			<span>{m['pages.home.actions.hide']()}</span>
		</AcerolaButton>

		<!-- Delete Action (Destructive Confirmation Alert) -->
		<AcerolaButton
			ui={{
				variant: 'destructive',
				class: 'w-full justify-start rounded-xl',
				disabled: isProcessing || selectedIds.length === 0
			}}
			events={{ onClick: handleDeleteClick }}
		>
			<Trash2 size={18} class="mr-2" />
			<span>{m['pages.home.actions.delete']()}</span>
		</AcerolaButton>
	</div>
</AcerolaDialog>

<!-- Hide Confirmation Alert Dialog -->
<AcerolaAlertDialog
	state={{ open: showHideDialog }}
	data={{
		title: m['pages.home.actions.hide_confirm_title'](),
		description: m['pages.home.actions.hide_confirm_desc'](),
		cancelText: m['pages.home.actions.cancel'](),
		actionText: m['pages.home.actions.hide_confirm_action']()
	}}
	events={{
		onAction: handleHide,
		onCancel: () => (showHideDialog = false)
	}}
	ui={{ variant: 'destructive' }}
/>

<!-- Delete Confirmation Alert Dialog -->
<AcerolaAlertDialog
	state={{ open: showDeleteDialog }}
	data={{
		title: m['pages.home.actions.delete_confirm_title'](),
		description: m['pages.home.actions.delete_confirm_desc'](),
		cancelText: m['pages.home.actions.cancel'](),
		actionText: m['pages.home.actions.delete_confirm_action']()
	}}
	events={{
		onAction: handleDelete,
		onCancel: () => (showDeleteDialog = false)
	}}
	ui={{ variant: 'destructive' }}
/>
