import { error } from '@tauri-apps/plugin-log';

let selectedIds = $state<Set<number>>(new Set());
let isSelectionMode = $state(false);

/**
 * Hook para gerenciar seleção múltipla de quadrinhos.
 * Permite selecionar um ou vários quadrinhos para ações em batch.
 */
export function useComicSelection() {
	function toggleSelection(id: number): void {
		if (selectedIds.has(id)) {
			selectedIds.delete(id);
			if (selectedIds.size === 0) {
				isSelectionMode = false;
			}
		} else {
			selectedIds.add(id);
			isSelectionMode = true;
		}
	}

	function selectSingle(id: number): void {
		selectedIds.clear();
		selectedIds.add(id);
		isSelectionMode = true;
	}

	function selectAll(ids: number[]): void {
		selectedIds = new Set(ids);
		isSelectionMode = ids.length > 0;
	}

	function deselectAll(): void {
		selectedIds.clear();
		isSelectionMode = false;
	}

	function enterSelectionMode(): void {
		isSelectionMode = true;
	}

	function exitSelectionMode(): void {
		selectedIds.clear();
		isSelectionMode = false;
	}

	function isSelected(id: number): boolean {
		return selectedIds.has(id);
	}

	return {
		toggleSelection,
		selectSingle,
		selectAll,
		deselectAll,
		enterSelectionMode,
		exitSelectionMode,
		isSelected,
		get selectedIds() {
			return selectedIds;
		},
		get selectedCount() {
			return selectedIds.size;
		},
		get isSelectionMode() {
			return isSelectionMode;
		},
		get selectedIdsArray() {
			return Array.from(selectedIds);
		}
	};
}
