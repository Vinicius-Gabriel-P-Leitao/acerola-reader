import { invoke } from '@tauri-apps/api/core';
import { error } from '@tauri-apps/plugin-log';
import { ARCHIVE_TEMPLATE_COMMANDS } from '$lib/contracts/archive/archive-template.commands';
import type {
	ArchiveTemplate,
	ArchiveTemplateSortType
} from '$lib/contracts/archive/archive-template.payloads';

let templates = $state<ArchiveTemplate[]>([]);
let isLoading = $state(false);
let isInitialized = false;

// ONLY FOR TESTING
export function _resetArchiveTemplatesState() {
	templates = [];
	isInitialized = false;
	isLoading = false;
}

/**
 * Hook to manage archive naming templates in the global state.
 * Default (built-in) templates always come from the backend with `is_default: true`
 * and are never mutated here — only user-created templates can be created or deleted.
 *
 * @returns An object containing the templates state and mutation methods.
 */
export function useArchiveTemplates() {
	async function loadTemplates() {
		if (isInitialized) return;
		isLoading = true;
		try {
			templates = await invoke<ArchiveTemplate[]>(ARCHIVE_TEMPLATE_COMMANDS.getArchiveTemplates);
			isInitialized = true;
		} catch (err) {
			error(`Failed to load archive templates: ${err}`);
			throw err;
		} finally {
			isLoading = false;
		}
	}

	async function createTemplate(label: string, pattern: string, sortType: ArchiveTemplateSortType) {
		try {
			const created = await invoke<ArchiveTemplate>(
				ARCHIVE_TEMPLATE_COMMANDS.createArchiveTemplate,
				{ label, pattern, sortType }
			);
			templates = [...templates, created];
			return created;
		} catch (err) {
			error(`Failed to create archive template: ${err}`);
			throw err;
		}
	}

	async function deleteTemplate(id: number) {
		try {
			await invoke(ARCHIVE_TEMPLATE_COMMANDS.deleteArchiveTemplate, { id: Number(id) });
			templates = templates.filter((template) => template.id !== id);
		} catch (err) {
			error(`Failed to delete archive template: ${err}`);
			throw err;
		}
	}

	return {
		get templates() {
			return templates;
		},
		get isLoading() {
			return isLoading;
		},
		loadTemplates,
		createTemplate,
		deleteTemplate
	};
}
