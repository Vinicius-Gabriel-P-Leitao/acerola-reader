import { browser } from '$app/environment';
import { replaceState } from '$app/navigation';
import { page } from '$app/state';
import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';
import type { ReaderChapterPayload } from '$lib/contracts/reader/reader.payloads';
import { LIBRARY_EVENTS } from '$lib/contracts/library/chapter.events';
import { LIBRARY_COMMANDS } from '$lib/contracts/library/chapter.commands';
import { SESSION_KEYS } from '$lib/constants/session-keys';
import type { ChapterPayload } from '$lib/contracts/library/chapter.payloads';
import { onMount } from 'svelte';

export type ReaderNavigationState = {
	chapter?: ReaderChapterPayload;
	startPage?: number;
	chapterIndex?: number;
	totalChapters?: number;
	chapterScope?: string;
	comicDirectoryId?: string;
};

export function useReaderNavigation() {
	let state = $state<ReaderNavigationState>({} as ReaderNavigationState);
	let initializing = $state(false);

	// Mantém o rastro da navegação pendente para que o listener saiba o que fazer
	let pendingAction = $state<'load' | 'navigate' | null>(null);
	let pendingTargetIndex = $state<number | null>(null);

	if (browser) {
		const pageState = (page.state ?? {}) as ReaderNavigationState;

		if (Object.keys(pageState).length > 0 && pageState.chapter) {
			state = pageState;
			sessionStorage.setItem(SESSION_KEYS.readerState, JSON.stringify(pageState));
		} else {
			const saved = sessionStorage.getItem(SESSION_KEYS.readerState);

			if (saved) {
				state = JSON.parse(saved);
			}
		}
	} else {
		state = (page.state ?? {}) as ReaderNavigationState;
	}

	$effect(() => {
		const pageState = (page.state ?? {}) as ReaderNavigationState;
		
		if (Object.keys(pageState).length > 0 && pageState.chapter) {
			state = pageState;
			sessionStorage.setItem(SESSION_KEYS.readerState, JSON.stringify(pageState));
		}
	});

	const chapter = $derived(state.chapter);

	const chapterIndex = $derived.by(() => {
		const value = state.chapterIndex;
		return typeof value === 'number' && Number.isFinite(value) ? Math.max(0, value) : undefined;
	});

	const totalChapters = $derived.by(() => {
		const value = state.totalChapters;
		return typeof value === 'number' && Number.isFinite(value) ? Math.max(0, value) : undefined;
	});

	const chaptersRemaining = $derived.by(() => {
		if (chapterIndex === undefined || totalChapters === undefined) return undefined;
		return Math.max(0, totalChapters - chapterIndex - 1);
	});

	const hasNextChapter = $derived(chaptersRemaining !== undefined && chaptersRemaining > 0);
	const hasPreviousChapter = $derived(chapterIndex !== undefined && chapterIndex > 0);

	onMount(() => {
		let unlistenChapters: (() => void) | undefined;
		let unlistenError: (() => void) | undefined;

		const setupListeners = async () => {
			unlistenChapters = await listen<ChapterPayload>(LIBRARY_EVENTS.comicChapters, (event) => {
				const payload = event.payload;

				if (pendingAction === 'load') {
					const items = payload.archive.items;
					const idx = items.findIndex((item) => item.id === state.chapter?.id);
					if (idx !== -1) {
						state.chapterIndex = idx;
						state.totalChapters = payload.archive.total ?? items.length;
						sessionStorage.setItem(SESSION_KEYS.readerState, JSON.stringify(state));
					}
					pendingAction = null;
				} else if (pendingAction === 'navigate' && pendingTargetIndex !== null) {
					const nextChapterData = payload.archive.items[0];
					if (nextChapterData) {
						const readerChapter: ReaderChapterPayload = {
							id: nextChapterData.id,
							name: nextChapterData.name,
							path: nextChapterData.path,
							chapterSort: nextChapterData.chapterSort ?? '',
							volumeId: nextChapterData.volumeId ?? null,
							volumeName: nextChapterData.volumeName ?? null,
							isSpecial: nextChapterData.isSpecial ?? false,
							lastModified: nextChapterData.lastModified ?? 0
						};

						const newState: ReaderNavigationState = {
							chapter: readerChapter,
							comicDirectoryId: state.comicDirectoryId,
							chapterIndex: pendingTargetIndex,
							totalChapters: state.totalChapters,
							chapterScope: state.chapterScope
						};

						state = newState;
						replaceState(page.url, newState);
					}
					initializing = false;
					pendingAction = null;
					pendingTargetIndex = null;
				}
			});

			unlistenError = await listen<any>(LIBRARY_EVENTS.comicChaptersError, () => {
				initializing = false;
				pendingAction = null;
				pendingTargetIndex = null;
			});
		};

		void setupListeners();

		return () => {
			unlistenChapters?.();
			unlistenError?.();
		};
	});

	async function loadContext() {
		if (!chapter || !state.comicDirectoryId) return;

		pendingAction = 'load';

		try {
			await invoke(LIBRARY_COMMANDS.getComicChapters, {
				comicDirectoryFk: state.comicDirectoryId,
				volumeId: null, // Sempre busca em todo o quadrinho para permitir transições contínuas entre volumes
				page: 0,
				pageSize: 99999,
				asc: true
			});
		} catch {
			pendingAction = null;
		}
	}

	async function goToNextChapter() {
		if (!hasNextChapter || chapterIndex === undefined || !state.comicDirectoryId) return;
		await navigateToRelativeChapter(chapterIndex + 1);
	}

	async function goToPreviousChapter() {
		if (!hasPreviousChapter || chapterIndex === undefined || !state.comicDirectoryId) return;
		await navigateToRelativeChapter(chapterIndex - 1);
	}

	async function navigateToRelativeChapter(targetIndex: number) {
		initializing = true;
		pendingAction = 'navigate';
		pendingTargetIndex = targetIndex;

		try {
			await invoke(LIBRARY_COMMANDS.getComicChapters, {
				comicDirectoryFk: state.comicDirectoryId,
				volumeId: null,
				page: targetIndex,
				pageSize: 1,
				asc: true
			});
		} catch {
			initializing = false;
			pendingAction = null;
			pendingTargetIndex = null;
		}
	}

	return {
		get state() {
			return state;
		},
		get initializing() {
			return initializing;
		},
		set initializing(value) {
			initializing = value;
		},
		get chapter() {
			return chapter;
		},
		get chapterIndex() {
			return chapterIndex;
		},
		get totalChapters() {
			return totalChapters;
		},
		get chaptersRemaining() {
			return chaptersRemaining;
		},
		get hasNextChapter() {
			return hasNextChapter;
		},
		get hasPreviousChapter() {
			return hasPreviousChapter;
		},
		loadContext,
		goToNextChapter,
		goToPreviousChapter
	};
}
