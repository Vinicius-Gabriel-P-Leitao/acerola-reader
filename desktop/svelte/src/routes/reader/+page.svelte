<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import type { ReaderChapterPayload } from '$lib/contracts/reader/reader.payloads';
	import { useReader } from '$lib/hooks/store/use-reader.svelte';
	import { m } from '$lib/paraglide/messages';
	import { invoke } from '@tauri-apps/api/core';
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

	const navigationState = $derived((page.state ?? {}) as ReaderNavigationState);
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

	let scrollTimeoutId: number;

	onMount(() => {
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

		const start = async () => {
			if (!chapter) return;

			try {
				await reader.open(chapter, navigationState.startPage ?? 0);
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
		};

		void start();

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
			}).catch((err) => console.error('Failed to update history', err));
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
				onReadingModeChange: (mode) => (readingMode = mode),
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
				chaptersRemainingLabel
			}}
			state={{ readingMode }}
			events={{ onReadingModeChange: (mode) => (readingMode = mode) }}
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
				onReadingModeChange: (mode) => (readingMode = mode),
				onToggleZoomMode: zoom.toggleZoomMode,
				onZoomIn: zoom.zoomIn,
				onZoomOut: zoom.zoomOut,
				onResetZoom: zoom.resetZoom
			}}
		/>
	{/snippet}
</ReaderShell>
