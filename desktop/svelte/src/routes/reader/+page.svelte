<script lang="ts">
	import { browser } from '$app/environment';
	import { goto, replaceState } from '$app/navigation';
	import { page } from '$app/state';
	import type { ReaderChapterPayload } from '$lib/contracts/reader/reader.payloads';
	import { useReader } from '$lib/hooks/store/use-reader.svelte';
	import { useReaderMode } from '$lib/hooks/preferences/use-reader-mode.svelte';
	import { m } from '$lib/paraglide/messages';
	import { invoke } from '@tauri-apps/api/core';
	import { listen } from '@tauri-apps/api/event';
	import { LIBRARY_EVENTS } from '$lib/contracts/library/chapter.events';
	import { LIBRARY_COMMANDS } from '$lib/contracts/library/chapter.commands';
	import { onMount, tick, untrack } from 'svelte';
	import ReaderCommandPalette from './components/reader-command-palette.svelte';
	import ReaderFooter from './components/reader-footer.svelte';
	import ReaderPages from './components/reader-pages.svelte';
	import ReaderShell from './components/reader-shell.svelte';
	import ReaderToolbar from './components/reader-toolbar.svelte';
	import ReaderViewport from './components/reader-viewport.svelte';
	import {
		isReaderEditableTarget,
		type ReaderMode,
		useReaderZoom
	} from './hooks/use-reader-zoom.svelte';

	type ReaderNavigationState = {
		chapter?: ReaderChapterPayload;
		startPage?: number;
		chapterIndex?: number;
		totalChapters?: number;
		chapterScope?: string;
		comicDirectoryId?: string;
	};

	const reader = useReader();
	const zoom = useReaderZoom();
	const readerModeStore = useReaderMode();

	let observer: IntersectionObserver | null = null;
	let visibleRects = new Map<number, DOMRectReadOnly>();
	let pageNodes = new Map<number, HTMLElement>();
	let visiblePages = $state<number[]>([]);
	let openFailed = $state(false);
	let readingMode = $state<ReaderMode>('vertical');
	let modeSwitchPage = $state<number | null>(null);
	let commandOpen = $state(false);
	let commandValue = $state('');
	let initializing = $state(true);

	function updateReadingMode(mode: ReaderMode) {
		readingMode = mode;
		void readerModeStore.saveReaderMode(mode);
	}

	async function goToNextChapter() {
		if (chaptersRemaining === undefined || chaptersRemaining <= 0) return;
		if (chapterIndex === undefined || !navigationState.comicDirectoryId) return;

		initializing = true;

		const unlisten = await listen<any>(LIBRARY_EVENTS.comicChapters, (event) => {
			const payload = event.payload;
			const nextChapterData = payload.archive.items[0];
			
			if (nextChapterData) {
				const readerChapter: ReaderChapterPayload = {
					id: nextChapterData.id,
					name: nextChapterData.name ?? nextChapterData.title,
					path: nextChapterData.path,
					chapterSort: nextChapterData.chapterSort ?? '',
					volumeId: nextChapterData.volumeId ?? null,
					volumeName: nextChapterData.volumeName ?? null,
					isSpecial: nextChapterData.isSpecial ?? false,
					lastModified: nextChapterData.lastModified ?? 0
				};
				
				const newState = {
					chapter: readerChapter,
					comicDirectoryId: navigationState.comicDirectoryId,
					chapterIndex: chapterIndex + 1,
					totalChapters: totalChapters,
					chapterScope: navigationState.chapterScope
				};

				navigationState = newState;
				replaceState(page.url, newState);
			} else {
				initializing = false;
			}
			unlisten();
		});

		await invoke(LIBRARY_COMMANDS.getComicChapters, {
			comicDirectoryFk: navigationState.comicDirectoryId,
			volumeId: null,
			page: chapterIndex + 1,
			pageSize: 1,
			asc: true
		}).catch(() => {
			initializing = false;
			unlisten();
		});
	}

	const hasPreviousChapter = $derived(
		chapterIndex !== undefined && chapterIndex > 0
	);

	async function goToPreviousChapter() {
		if (chapterIndex === undefined || chapterIndex <= 0 || !navigationState.comicDirectoryId) return;

		initializing = true;

		const unlisten = await listen<any>(LIBRARY_EVENTS.comicChapters, (event) => {
			const payload = event.payload;
			const prevChapterData = payload.archive.items[0];
			
			if (prevChapterData) {
				const readerChapter: ReaderChapterPayload = {
					id: prevChapterData.id,
					name: prevChapterData.name ?? prevChapterData.title,
					path: prevChapterData.path,
					chapterSort: prevChapterData.chapterSort ?? '',
					volumeId: prevChapterData.volumeId ?? null,
					volumeName: prevChapterData.volumeName ?? null,
					isSpecial: prevChapterData.isSpecial ?? false,
					lastModified: prevChapterData.lastModified ?? 0
				};
				
				const newState = {
					chapter: readerChapter,
					comicDirectoryId: navigationState.comicDirectoryId,
					chapterIndex: chapterIndex - 1,
					totalChapters: totalChapters,
					chapterScope: navigationState.chapterScope
				};

				navigationState = newState;
				replaceState(page.url, newState);
			} else {
				initializing = false;
			}
			unlisten();
		});

		await invoke(LIBRARY_COMMANDS.getComicChapters, {
			comicDirectoryFk: navigationState.comicDirectoryId,
			volumeId: null,
			page: chapterIndex - 1,
			pageSize: 1,
			asc: true
		}).catch(() => {
			initializing = false;
			unlisten();
		});
	}

	let navigationState = $state<ReaderNavigationState>({} as ReaderNavigationState);

	if (browser) {
		const state = (page.state ?? {}) as ReaderNavigationState;
		if (Object.keys(state).length > 0 && state.chapter) {
			navigationState = state;
			sessionStorage.setItem('acerola:reader_state', JSON.stringify(state));
		} else {
			const saved = sessionStorage.getItem('acerola:reader_state');
			if (saved) {
				navigationState = JSON.parse(saved);
			}
		}
	} else {
		navigationState = (page.state ?? {}) as ReaderNavigationState;
	}

	$effect(() => {
		const state = (page.state ?? {}) as ReaderNavigationState;
		if (Object.keys(state).length > 0 && state.chapter) {
			navigationState = state;
			sessionStorage.setItem('acerola:reader_state', JSON.stringify(state));
		}
	});

	const chapter = $derived(navigationState.chapter);

	const chapterIndex = $derived.by(() => {
		const value = navigationState.chapterIndex;
		return typeof value === 'number' && Number.isFinite(value) ? Math.max(0, value) : undefined;
	});

	const totalChapters = $derived.by(() => {
		const value = navigationState.totalChapters;
		return typeof value === 'number' && Number.isFinite(value) ? Math.max(0, value) : undefined;
	});

	const chaptersRemaining = $derived.by(() => {
		if (chapterIndex === undefined || totalChapters === undefined) return undefined;
		return Math.max(0, totalChapters - chapterIndex - 1);
	});

	const hasNextChapter = $derived(
		chaptersRemaining !== undefined && chaptersRemaining > 0
	);

	const chapterProgressLabel = $derived.by(() => {
		if (chapterIndex === undefined || totalChapters === undefined) {
			return m['pages.reader.progress.chapter_unavailable']();
		}

		return m['pages.reader.progress.chapter_count']({
			current: chapterIndex + 1,
			total: totalChapters
		});
	});

	const chaptersRemainingLabel = $derived.by(() => {
		if (chaptersRemaining === undefined) {
			return m['pages.reader.progress.remaining_unavailable']();
		}

		return chaptersRemaining === 1
			? m['pages.reader.progress.remaining_one']()
			: m['pages.reader.progress.remaining_many']({ count: chaptersRemaining });
	});

	const pageProgressPercent = $derived(
		reader.pageCount > 0 ? Math.round(((reader.currentPage + 1) / reader.pageCount) * 100) : 0
	);

	const pageProgressWidth = $derived(`${pageProgressPercent}%`);
	const readerScopeLabel = $derived(navigationState.chapterScope ?? chapterProgressLabel);

	const readerSubtitle = $derived(
		reader.session
			? m['pages.reader.progress.subtitle']({
					scope: readerScopeLabel,
					current: reader.currentPage + 1,
					total: reader.pageCount
				})
			: undefined
	);

	const isPaginatedMode = $derived(readingMode !== 'webtoon');
	const pageControlsDisabled = $derived(isPaginatedMode && zoom.isZoomed);
	const canPreviousPage = $derived(Boolean(reader.session) && reader.currentPage > 0);

	const canNextPage = $derived(
		Boolean(reader.session) && reader.currentPage < reader.pageCount - 1
	);

	const modeLabel = $derived.by(() => {
		if (readingMode === 'horizontal') return m['pages.reader.modes.horizontal']();
		if (readingMode === 'webtoon') return m['pages.reader.modes.webtoon']();
		return m['pages.reader.modes.vertical']();
	});

	function leaveReader() {
		if (window.history.length > 1) {
			window.history.back();
			return;
		}

		goto('/home');
	}

	function openCommandPalette() {
		commandValue = '';
		commandOpen = true;
	}

	function handleKeydown(event: KeyboardEvent) {
		const key = event.key.toLowerCase();
		const commandKey = event.ctrlKey || event.metaKey;

		if (commandKey && key === 'k') {
			event.preventDefault();
			commandOpen ? (commandOpen = false) : openCommandPalette();
			return;
		}

		if (commandOpen && key === 'escape') {
			event.preventDefault();
			commandOpen = false;
			return;
		}

		if (isReaderEditableTarget(event.target)) return;

		if (key === 'arrowright' && canNextPage && !pageControlsDisabled) {
			event.preventDefault();
			void goToReaderPage(reader.currentPage + 1);
			return;
		}

		if (key === 'arrowleft' && canPreviousPage && !pageControlsDisabled) {
			event.preventDefault();
			void goToReaderPage(reader.currentPage - 1);
			return;
		}

		if (commandKey && (event.key === '+' || event.key === '=')) {
			event.preventDefault();
			zoom.zoomIn();
			return;
		}

		if (commandKey && event.key === '-') {
			event.preventDefault();
			zoom.zoomOut();
			return;
		}

		if (commandKey && event.key === '0') {
			event.preventDefault();
			zoom.resetZoom();
			return;
		}

		if (key === 'z') {
			event.preventDefault();
			zoom.toggleZoomMode();
		}
	}

	function visiblePageOrder() {
		const horizontal = readingMode === 'horizontal';
		const viewportCenter = horizontal ? window.innerWidth / 2 : window.innerHeight / 2;

		return Array.from(visibleRects.entries())
			.sort(([, leftRect], [, rightRect]) => {
				const leftCenter = horizontal
					? leftRect.left + leftRect.width / 2
					: leftRect.top + leftRect.height / 2;

				const rightCenter = horizontal
					? rightRect.left + rightRect.width / 2
					: rightRect.top + rightRect.height / 2;

				return Math.abs(leftCenter - viewportCenter) - Math.abs(rightCenter - viewportCenter);
			})
			.map(([pageIndex]) => pageIndex);
	}

	function scrollPageIntoView(pageIndex: number, behavior: ScrollBehavior = 'smooth') {
		const node = pageNodes.get(pageIndex);
		if (!node) return;

		node.scrollIntoView({
			behavior,
			block: readingMode === 'webtoon' ? 'start' : 'center',
			inline: readingMode === 'horizontal' ? 'center' : 'nearest'
		});
	}

	async function goToReaderPage(pageIndex: number) {
		if (!reader.session || reader.pageCount === 0) return;

		const targetPage = Math.max(0, Math.min(pageIndex, reader.pageCount - 1));
		const distance = Math.abs(reader.currentPage - targetPage);
		
		await reader.goToPage(targetPage);
		zoom.resetPan();

		await tick();
		// If jumping far, don't smooth scroll to avoid firing observer on 50 intermediate pages
		scrollPageIntoView(targetPage, distance > 3 ? 'auto' : 'smooth');
	}

	async function start() {
		if (!chapter) {
			initializing = false;
			return;
		}

		if (navigationState.comicDirectoryId) {
			const unlisten = await listen<any>(LIBRARY_EVENTS.comicChapters, (event) => {
				const payload = event.payload;
				const items = payload.archive.items;
				const idx = items.findIndex((chapterItem: any) => chapterItem.id === chapter.id);
				if (idx !== -1) {
					navigationState.chapterIndex = idx;
					navigationState.totalChapters = payload.archive.total;
					sessionStorage.setItem('acerola:reader_state', JSON.stringify(navigationState));
				}
				unlisten();
			});

			await invoke(LIBRARY_COMMANDS.getComicChapters, {
				comicDirectoryFk: navigationState.comicDirectoryId,
				volumeId: null, // Always search across the whole comic to allow seamless volume transitions
				page: 0,
				pageSize: 99999,
				asc: true
			}).catch(() => {
				unlisten();
			});
		}

		try {
			await reader.open(chapter, navigationState.startPage ?? 0);
			navigationState.startPage = undefined; // Reset start page so subsequent loads start at 0
			await tick();
			
			// Small delay to ensure DOM is ready and IntersectionObserver catches the initial state
			setTimeout(() => {
				scrollPageIntoView(reader.currentPage, 'auto');
				
				// Another tick to allow the observer to register the jump before showing UI
				setTimeout(() => {
					initializing = false;
				}, 50);
			}, 50);
		} catch {
			openFailed = true;
			initializing = false;
		}
	}

	let currentChapterId = $state<string | null>(null);

	$effect(() => {
		if (chapter && chapter.id !== currentChapterId) {
			untrack(() => {
				currentChapterId = chapter.id;
				initializing = true;
				void start();
			});
		}
	});

	let scrollTimeoutId: number;

	onMount(() => {
		void readerModeStore.loadReaderMode().then(() => {
			readingMode = readerModeStore.readerMode;
		});

		observer = new IntersectionObserver(
			(entries) => {
				let changed = false;

				for (const entry of entries) {
					const pageIndex = Number((entry.target as HTMLElement).dataset.page);
					if (!Number.isFinite(pageIndex)) continue;

					if (entry.isIntersecting) {
						visibleRects.set(pageIndex, entry.boundingClientRect);
					} else {
						visibleRects.delete(pageIndex);
					}

					changed = true;
				}

				if (changed) {
					window.clearTimeout(scrollTimeoutId);
					scrollTimeoutId = window.setTimeout(() => {
						visiblePages = visiblePageOrder();
					}, 50);
				}
			},
			{ threshold: 0.01 }
		);

		return () => observer?.disconnect();
	});

	$effect(() => {
		const mode = readingMode;
		if (!reader.session) return;

		untrack(() => {
			const targetPage = reader.currentPage;
			modeSwitchPage = targetPage;
			zoom.forceResetZoom();
			visibleRects.clear();
			visiblePages = [];

			void tick().then(() => {
				if (mode === readingMode) {
					scrollPageIntoView(targetPage, 'auto');

					window.setTimeout(() => {
						if (mode === readingMode && modeSwitchPage === targetPage) {
							modeSwitchPage = null;
						}
					}, 120);
				}
			});
		});
	});

	$effect(() => {
		if (!reader.session || visiblePages.length === 0) return;
		if (zoom.isZoomed) return;

		if (modeSwitchPage !== null) {
			const targetPage = modeSwitchPage;

			untrack(() => {
				void reader.loadPage(targetPage, false).catch(() => undefined);
			});
			return;
		}

		const targetPage = visiblePages[0];

		untrack(() => {
			if (targetPage !== reader.currentPage) {
				void reader.goToPage(targetPage);
			}

			for (const pageIndex of visiblePages) {
				void reader.loadPage(pageIndex, false).catch(() => undefined);
			}
		});
	});

	function trackPage(node: HTMLElement, pageIndex: number) {
		node.dataset.page = pageIndex.toString();
		pageNodes.set(pageIndex, node);
		observer?.observe(node);

		return {
			destroy() {
				observer?.unobserve(node);
				visibleRects.delete(pageIndex);

				if (pageNodes.get(pageIndex) === node) {
					pageNodes.delete(pageIndex);
				}
			}
		};
	}

	$effect(() => {
		const currentPage = reader.currentPage;
		const pageCount = reader.pageCount;
		const chapterId = chapter?.id;
		const comicDirectoryId = navigationState.comicDirectoryId;

		if (!reader.session || !chapterId || !comicDirectoryId || pageCount === 0) return;

		untrack(() => {
			const isCompleted = (currentPage + 1) / pageCount >= 0.7;
			invoke('history_update_reading', {
				comicId: comicDirectoryId.toString(),
				chapterId: chapterId.toString(),
				lastPage: currentPage,
				isCompleted
			}).catch((error) => console.error('Failed to update history', error));
		});
	});
</script>

<svelte:window onkeydown={handleKeydown} onresize={zoom.clampPan} />

{#if initializing}
	<div class="fixed inset-0 z-[100] flex flex-col items-center justify-center bg-base/95 backdrop-blur-xl transition-opacity duration-300">
		<div class="flex flex-col items-center gap-4">
			<div class="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
			<p class="text-sm font-black tracking-widest text-primary uppercase animate-pulse">{m['pages.reader.fallback.loading']()}</p>
		</div>
	</div>
{/if}

<ReaderShell>
	{#snippet toolbar()}
		<ReaderToolbar
			data={{
				title: chapter?.name ?? m['pages.reader.fallback.chapter_unavailable'](),
				subtitle: readerSubtitle,
				zoomLevel: zoom.zoomLevel,
				zoomMode: zoom.zoomMode,
				isPaginatedMode,
				pageControlsDisabled,
				canPreviousPage,
				canNextPage
			}}
			state={{ readingMode }}
			events={{
				onBack: leaveReader,
				onReadingModeChange: updateReadingMode,
				onToggleQuickZoom: zoom.toggleQuickZoom,
				onToggleZoomMode: zoom.toggleZoomMode,
				onOpenCommandPalette: openCommandPalette,
				onPreviousPage: () => goToReaderPage(reader.currentPage - 1),
				onNextPage: () => goToReaderPage(reader.currentPage + 1)
			}}
		/>
	{/snippet}

	{#snippet viewport()}
		<ReaderViewport data={{ mode: readingMode }} context={{ zoom }}>
			<ReaderPages
				data={{
					mode: readingMode,
					pageCount: reader.pageCount,
					currentPage: reader.currentPage,
					openFailed,
					chapterAvailable: Boolean(chapter)
				}}
				services={{
					pageAt: reader.pageAt,
					trackPage
				}}
			/>
		</ReaderViewport>
	{/snippet}

	{#snippet footer()}
		<ReaderFooter
			data={{
				pageProgressPercent,
				pageProgressWidth,
				chapterProgressLabel,
				modeLabel,
				zoomStatusLabel: zoom.zoomStatusLabel,
				chaptersRemainingLabel,
				hasNextChapter,
				hasPreviousChapter
			}}
			state={{ readingMode }}
			events={{ 
				onReadingModeChange: updateReadingMode,
				onNextChapter: goToNextChapter,
				onPreviousChapter: goToPreviousChapter
			}}
		/>
	{/snippet}

	{#snippet command()}
		<ReaderCommandPalette
			data={{ zoomMode: zoom.zoomMode }}
			state={{
				open: commandOpen,
				value: commandValue,
				readingMode
			}}
			events={{
				onOpenChange: (open) => (commandOpen = open),
				onValueChange: (value) => (commandValue = value),
				onReadingModeChange: updateReadingMode,
				onToggleZoomMode: zoom.toggleZoomMode,
				onZoomIn: zoom.zoomIn,
				onZoomOut: zoom.zoomOut,
				onResetZoom: zoom.resetZoom
			}}
		/>
	{/snippet}
</ReaderShell>
