<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import AcerolaButtonIcon from '$lib/components/acerola-button/acerola-button-icon.svelte';
	import AcerolaCommand from '$lib/components/acerola-command/acerola-command.svelte';
	import AcerolaToggleGroup from '$lib/components/acerola-toggle-group/acerola-toggle-group.svelte';
	import * as Command from '$lib/components/ui/command';
	import { ToggleGroupItem } from '$lib/components/ui/toggle-group/index.js';
	import { useReader } from '$lib/hooks/store/use-reader.svelte';
	import type { ReaderChapterPayload } from '$lib/contracts/reader/reader.payloads';
	import ArrowLeft from '@lucide/svelte/icons/arrow-left';
	import ChevronLeft from '@lucide/svelte/icons/chevron-left';
	import ChevronRight from '@lucide/svelte/icons/chevron-right';
	import Columns2 from '@lucide/svelte/icons/columns-2';
	import CommandIcon from '@lucide/svelte/icons/command';
	import RefreshCw from '@lucide/svelte/icons/refresh-cw';
	import Rows2 from '@lucide/svelte/icons/rows-2';
	import ScrollText from '@lucide/svelte/icons/scroll-text';
	import ZoomIn from '@lucide/svelte/icons/zoom-in';
	import ZoomOut from '@lucide/svelte/icons/zoom-out';
	import { onMount, tick, untrack } from 'svelte';
	import { fade } from 'svelte/transition';

	type ReaderMode = 'vertical' | 'horizontal' | 'webtoon';

	const MIN_ZOOM = 1;
	const MAX_ZOOM = 3;
	const QUICK_ZOOM = 1.65;
	const ZOOM_STEP = 0.15;

	type ReaderNavigationState = {
		chapter?: ReaderChapterPayload;
		startPage?: number;
		chapterIndex?: number;
		totalChapters?: number;
		chapterScope?: string;
	};

	const reader = useReader();

	let observer: IntersectionObserver | null = null;
	let visibleRects = new Map<number, DOMRectReadOnly>();
	let pageNodes = new Map<number, HTMLElement>();
	let visiblePages = $state<number[]>([]);
	let openFailed = $state(false);
	let readingMode = $state<ReaderMode>('vertical');
	let zoomLevel = $state(1);
	let zoomMode = $state(false);
	let panX = $state(0);
	let panY = $state(0);
	let zoomOriginX = $state(0);
	let zoomOriginY = $state(0);
	let isPanning = $state(false);
	let panStartX = 0;
	let panStartY = 0;
	let panOriginX = 0;
	let panOriginY = 0;
	let readerViewport = $state<HTMLElement | null>(null);
	let modeSwitchPage = $state<number | null>(null);
	let commandOpen = $state(false);
	let commandValue = $state('');

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
		if (chapterIndex === undefined || totalChapters === undefined) return 'Progresso do capitulo';
		return `Capitulo ${chapterIndex + 1} de ${totalChapters}`;
	});
	const chaptersRemainingLabel = $derived.by(() => {
		if (chaptersRemaining === undefined) return 'Capitulos restantes indisponiveis';
		return chaptersRemaining === 1
			? '1 capitulo restante'
			: `${chaptersRemaining} capitulos restantes`;
	});
	const pageProgressPercent = $derived(
		reader.pageCount > 0 ? Math.round(((reader.currentPage + 1) / reader.pageCount) * 100) : 0
	);
	const pageProgressWidth = $derived(`${pageProgressPercent}%`);
	const zoomPercent = $derived(Math.round(zoomLevel * 100));
	const zoomLabel = $derived(`${zoomPercent}%`);
	const zoomStatusLabel = $derived(
		zoomMode ? `Zoom ${zoomLabel} - scroll ajusta` : `Zoom ${zoomLabel}`
	);
	const isPaginatedMode = $derived(readingMode !== 'webtoon');
	const pageControlsDisabled = $derived(isPaginatedMode && zoomLevel > 1);
	const zoomLayerStyle = $derived(
		`transform: translate3d(${panX}px, ${panY}px, 0) scale(${zoomLevel}); transform-origin: ${zoomOriginX}px ${zoomOriginY}px;`
	);
	const readerScopeLabel = $derived(navigationState.chapterScope ?? chapterProgressLabel);
	const modeLabel = $derived.by(() => {
		if (readingMode === 'horizontal') return 'Paginado horizontal';
		if (readingMode === 'webtoon') return 'Webtoon';
		return 'Paginado vertical';
	});
	const mainClass = $derived.by(() => {
		const cursorClass = isPanning
			? 'cursor-grabbing'
			: zoomLevel > 1
				? 'cursor-grab'
				: zoomMode
					? 'cursor-zoom-in'
					: '';

		if (zoomLevel > 1) {
			return `scrollbar-hide flex-1 overflow-hidden bg-mantle/30 ${cursorClass}`;
		}

		if (readingMode === 'horizontal') {
			return `scrollbar-hide flex-1 snap-x snap-mandatory overflow-x-auto overflow-y-hidden bg-mantle/30 scroll-smooth ${cursorClass}`;
		}

		if (readingMode === 'vertical') {
			return `scrollbar-hide flex-1 snap-y snap-mandatory overflow-x-hidden overflow-y-auto bg-mantle/30 scroll-smooth ${cursorClass}`;
		}

		return `scrollbar-hide flex-1 overflow-x-hidden overflow-y-auto bg-mantle/30 scroll-smooth ${cursorClass}`;
	});
	const zoomLayerClass = $derived.by(() => {
		const motionClass = isPanning ? '' : 'transition-transform duration-150 ease-out';

		if (readingMode === 'horizontal') return `h-full w-max will-change-transform ${motionClass}`;
		if (readingMode === 'webtoon') return `w-full will-change-transform ${motionClass}`;

		return `h-full w-full will-change-transform ${motionClass}`;
	});
	const readerContentClass = $derived.by(() => {
		if (readingMode === 'horizontal') return 'flex h-full w-max';
		if (readingMode === 'webtoon')
			return 'mx-auto flex w-full max-w-3xl flex-col items-center gap-0 py-2';

		return 'mx-auto flex h-full w-full max-w-6xl flex-col items-center';
	});
	const pageShellClass = $derived.by(() => {
		if (readingMode === 'horizontal') {
			return 'flex h-full w-screen shrink-0 snap-center items-center justify-center px-5 py-6';
		}

		if (readingMode === 'webtoon') {
			return 'flex w-full items-start justify-center';
		}

		return 'flex h-full w-full shrink-0 snap-center items-center justify-center px-3 py-6';
	});
	const imageBoxClass = $derived.by(() => {
		if (readingMode === 'webtoon') {
			return 'mx-auto flex w-full justify-center';
		}

		return 'flex h-full w-full items-center justify-center';
	});
	const imageClass = $derived.by(() => {
		if (readingMode === 'webtoon') {
			return 'w-full bg-base object-contain';
		}

		return 'max-h-full max-w-full bg-base object-contain shadow-2xl shadow-base/40';
	});
	const placeholderClass = $derived.by(() => {
		if (readingMode !== 'webtoon') {
			return 'flex h-full w-full max-w-4xl items-center justify-center border border-surface/40 bg-base/50';
		}

		return 'flex min-h-[64vh] w-full items-center justify-center border-y border-surface/40 bg-base/50';
	});

	function leaveReader() {
		if (window.history.length > 1) {
			window.history.back();
			return;
		}

		goto('/home');
	}

	function resetPan() {
		panX = 0;
		panY = 0;
	}

	function panBounds() {
		const rect = readerViewport?.getBoundingClientRect();
		const width = rect?.width ?? 0;
		const height = rect?.height ?? 0;
		const extraX = Math.max(0, (width * (zoomLevel - 1)) / 2);
		const extraY = Math.max(0, (height * (zoomLevel - 1)) / 2);

		return {
			minX: -extraX,
			maxX: extraX,
			minY: -extraY,
			maxY: extraY
		};
	}

	function clampPan() {
		const bounds = panBounds();

		panX = Math.min(bounds.maxX, Math.max(bounds.minX, panX));
		panY = Math.min(bounds.maxY, Math.max(bounds.minY, panY));
	}

	function clampZoom(value: number) {
		return Math.round(Math.max(MIN_ZOOM, Math.min(value, MAX_ZOOM)) * 100) / 100;
	}

	function zoomAnchorPoint(anchor?: MouseEvent | WheelEvent) {
		const viewport = readerViewport;

		if (!viewport) {
			return {
				x: window.innerWidth / 2,
				y: window.innerHeight / 2
			};
		}

		const rect = viewport.getBoundingClientRect();

		if (anchor) {
			return {
				x: viewport.scrollLeft + anchor.clientX - rect.left,
				y: viewport.scrollTop + anchor.clientY - rect.top
			};
		}

		return {
			x: viewport.scrollLeft + rect.width / 2,
			y: viewport.scrollTop + rect.height / 2
		};
	}

	function setZoom(value: number, anchor?: MouseEvent | WheelEvent) {
		const currentZoom = zoomLevel;
		const nextZoom = clampZoom(value);

		if (nextZoom === currentZoom) return;

		if (currentZoom === MIN_ZOOM && nextZoom > MIN_ZOOM) {
			const point = zoomAnchorPoint(anchor);

			zoomOriginX = point.x;
			zoomOriginY = point.y;
			resetPan();
		}

		if (nextZoom === MIN_ZOOM) {
			zoomLevel = nextZoom;
			resetPan();
			return;
		}

		zoomLevel = nextZoom;
		clampPan();
	}

	function zoomIn(anchor?: MouseEvent | WheelEvent) {
		setZoom(zoomLevel + ZOOM_STEP, anchor);
	}

	function zoomOut(anchor?: MouseEvent | WheelEvent) {
		setZoom(zoomLevel - ZOOM_STEP, anchor);
	}

	function resetZoom() {
		setZoom(1);
		zoomMode = false;
	}

	function forceResetZoom() {
		zoomLevel = 1;
		zoomMode = false;
		resetPan();
		isPanning = false;
	}

	function toggleQuickZoom(anchor?: MouseEvent | WheelEvent) {
		setZoom(zoomLevel > 1 ? 1 : QUICK_ZOOM, anchor);
	}

	function toggleZoomMode() {
		if (zoomMode) {
			resetZoom();
			return;
		}

		zoomMode = !zoomMode;
	}

	function openCommandPalette() {
		commandValue = '';
		commandOpen = true;
	}

	function runCommand(action: () => void) {
		action();
		commandOpen = false;
		commandValue = '';
	}

	function isEditableTarget(target: EventTarget | null) {
		if (!(target instanceof HTMLElement)) return false;

		return (
			target.isContentEditable ||
			target.tagName === 'INPUT' ||
			target.tagName === 'TEXTAREA' ||
			target.tagName === 'SELECT'
		);
	}

	function handleReaderWheel(event: WheelEvent) {
		if (zoomLevel > 1 || zoomMode) {
			event.preventDefault();
		}

		if (!zoomMode) return;

		setZoom(zoomLevel + (event.deltaY < 0 ? ZOOM_STEP : -ZOOM_STEP), event);
	}

	function handleReaderPointerDown(event: PointerEvent) {
		if (zoomLevel <= 1 || event.button !== 0 || isEditableTarget(event.target)) return;

		event.preventDefault();
		isPanning = true;
		panStartX = event.clientX;
		panStartY = event.clientY;
		panOriginX = panX;
		panOriginY = panY;
		(event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
	}

	function handleReaderPointerMove(event: PointerEvent) {
		if (!isPanning) return;

		event.preventDefault();
		panX = panOriginX + event.clientX - panStartX;
		panY = panOriginY + event.clientY - panStartY;
		clampPan();
	}

	function stopReaderPan(event: PointerEvent) {
		if (!isPanning) return;

		isPanning = false;

		const target = event.currentTarget as HTMLElement;
		if (target.hasPointerCapture(event.pointerId)) {
			target.releasePointerCapture(event.pointerId);
		}
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

		if (isEditableTarget(event.target)) return;

		if (commandKey && (event.key === '+' || event.key === '=')) {
			event.preventDefault();
			zoomIn();
			return;
		}

		if (commandKey && event.key === '-') {
			event.preventDefault();
			zoomOut();
			return;
		}

		if (commandKey && event.key === '0') {
			event.preventDefault();
			resetZoom();
			return;
		}

		if (key === 'z') {
			event.preventDefault();
			toggleZoomMode();
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
		await reader.goToPage(targetPage);
		resetPan();

		await tick();
		scrollPageIntoView(targetPage);
	}

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
					visiblePages = visiblePageOrder();
				}
			},
			{ threshold: 0.01 }
		);

		const start = async () => {
			if (!chapter) return;

			try {
				await reader.open(chapter, navigationState.startPage ?? 0);
				await tick();
				scrollPageIntoView(reader.currentPage, 'auto');
			} catch {
				openFailed = true;
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
			forceResetZoom();
			visibleRects.clear();
			visiblePages = [];
			resetPan();

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
		if (zoomLevel > 1) return;

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
</script>

<svelte:window onkeydown={handleKeydown} onresize={clampPan} />

<div class="text-text fixed inset-x-0 top-8 bottom-0 z-50 flex flex-col overflow-hidden bg-base">
	<header
		class="relative z-20 flex min-h-16 shrink-0 items-center justify-between gap-3 border-b border-surface/40 bg-base/95 px-4 py-2 backdrop-blur-md"
	>
		<div class="flex min-w-0 flex-1 items-center gap-3">
			<AcerolaButtonIcon onclick={leaveReader} variant="ghost" title="Voltar">
				<ArrowLeft size={20} />
			</AcerolaButtonIcon>

			<div class="min-w-0">
				<p class="truncate text-sm font-black">{chapter?.name ?? 'Capitulo indisponivel'}</p>
				{#if reader.session}
					<p class="text-overlay truncate text-xs">
						{readerScopeLabel} - {reader.currentPage + 1} / {reader.pageCount} paginas
					</p>
				{/if}
			</div>
		</div>

		<AcerolaToggleGroup
			type="single"
			bind:value={readingMode}
			class="hidden shrink-0 gap-1 rounded-xl border border-surface/40 bg-mantle/60 p-1 md:flex"
		>
			{#snippet children()}
				<ToggleGroupItem
					value="vertical"
					title="Paginado vertical"
					class="h-9 gap-2 rounded-lg px-3 text-[10px] font-black tracking-widest uppercase data-[state=on]:bg-primary data-[state=on]:text-primary-foreground"
				>
					<Rows2 size={15} />
					<span class="hidden lg:inline">Vertical</span>
				</ToggleGroupItem>

				<ToggleGroupItem
					value="horizontal"
					title="Paginado horizontal"
					class="h-9 gap-2 rounded-lg px-3 text-[10px] font-black tracking-widest uppercase data-[state=on]:bg-primary data-[state=on]:text-primary-foreground"
				>
					<Columns2 size={15} />
					<span class="hidden lg:inline">Horizontal</span>
				</ToggleGroupItem>

				<ToggleGroupItem
					value="webtoon"
					title="Webtoon"
					class="h-9 gap-2 rounded-lg px-3 text-[10px] font-black tracking-widest uppercase data-[state=on]:bg-primary data-[state=on]:text-primary-foreground"
				>
					<ScrollText size={15} />
					<span class="hidden lg:inline">Webtoon</span>
				</ToggleGroupItem>
			{/snippet}
		</AcerolaToggleGroup>

		<div class="flex shrink-0 items-center gap-1">
			<AcerolaButtonIcon
				variant={zoomLevel > 1 ? 'secondary' : 'ghost'}
				onclick={() => toggleQuickZoom()}
				title={zoomLevel > 1 ? 'Resetar zoom' : 'Aplicar zoom'}
			>
				{#if zoomLevel > 1}
					<ZoomOut size={20} />
				{:else}
					<ZoomIn size={20} />
				{/if}
			</AcerolaButtonIcon>

			<AcerolaButtonIcon
				variant={zoomMode ? 'default' : 'ghost'}
				onclick={toggleZoomMode}
				title="Modo zoom"
			>
				<ZoomIn size={20} />
			</AcerolaButtonIcon>

			<AcerolaButtonIcon variant="ghost" onclick={openCommandPalette} title="Comandos">
				<CommandIcon size={20} />
			</AcerolaButtonIcon>

			{#if isPaginatedMode}
				<AcerolaButtonIcon
					variant="ghost"
					disabled={!reader.session || pageControlsDisabled || reader.currentPage <= 0}
					onclick={() => goToReaderPage(reader.currentPage - 1)}
					title={pageControlsDisabled ? 'Desative o zoom para trocar de pagina' : 'Pagina anterior'}
				>
					<ChevronLeft size={20} />
				</AcerolaButtonIcon>

				<AcerolaButtonIcon
					variant="ghost"
					disabled={!reader.session ||
						pageControlsDisabled ||
						reader.currentPage >= reader.pageCount - 1}
					onclick={() => goToReaderPage(reader.currentPage + 1)}
					title={pageControlsDisabled ? 'Desative o zoom para trocar de pagina' : 'Proxima pagina'}
				>
					<ChevronRight size={20} />
				</AcerolaButtonIcon>
			{/if}
		</div>
	</header>

	<main
		bind:this={readerViewport}
		class={mainClass}
		onwheel={handleReaderWheel}
		onpointerdown={handleReaderPointerDown}
		onpointermove={handleReaderPointerMove}
		onpointerup={stopReaderPan}
		onpointercancel={stopReaderPan}
		onlostpointercapture={stopReaderPan}
		ondblclick={(event) => toggleQuickZoom(event)}
	>
		{#if openFailed || !chapter}
			<div
				class="text-overlay flex h-full items-center justify-center text-sm font-black uppercase"
			>
				Capitulo indisponivel
			</div>
		{:else if reader.pageCount === 0}
			<div class="flex h-full items-center justify-center">
				<RefreshCw size={40} class="animate-spin text-primary" />
			</div>
		{:else}
			<div class={zoomLayerClass} style={zoomLayerStyle}>
				<div class={readerContentClass}>
					{#each Array.from({ length: reader.pageCount }) as _, pageIndex}
						{@const pageItem = reader.pageAt(pageIndex)}

						<section
							use:trackPage={pageIndex}
							class={pageShellClass}
							aria-label={`Pagina ${pageIndex + 1}`}
						>
							{#if pageItem}
								<div class={imageBoxClass}>
									<img
										in:fade={{ duration: 120 }}
										src={pageItem.url}
										alt={`Pagina ${pageIndex + 1}`}
										class={imageClass}
										loading={Math.abs(pageIndex - reader.currentPage) <= 2 ? 'eager' : 'lazy'}
										draggable="false"
									/>
								</div>
							{:else}
								<div class={placeholderClass}>
									<RefreshCw size={32} class="animate-spin text-primary" />
								</div>
							{/if}
						</section>
					{/each}
				</div>
			</div>
		{/if}
	</main>

	<footer
		class="relative z-20 shrink-0 border-t border-surface/40 bg-base/95 px-4 py-3 backdrop-blur-md"
	>
		<div class="flex flex-col gap-3">
			<div
				class="text-overlay flex items-center justify-between gap-3 text-[10px] font-black tracking-widest uppercase"
			>
				<span class="shrink-0">{pageProgressPercent}% lido</span>
				<span class="hidden min-w-0 truncate md:inline">{modeLabel} - {zoomStatusLabel}</span>
				<span class="min-w-0 truncate text-right">{chaptersRemainingLabel}</span>
			</div>

			<div
				role="progressbar"
				aria-valuemin="0"
				aria-valuemax="100"
				aria-valuenow={pageProgressPercent}
				class="h-2 overflow-hidden rounded-full bg-surface/60"
				title={chapterProgressLabel}
			>
				<div
					class="h-full rounded-full bg-primary transition-[width] duration-300 ease-out"
					style:width={pageProgressWidth}
				></div>
			</div>

			<AcerolaToggleGroup
				type="single"
				bind:value={readingMode}
				class="grid grid-cols-3 gap-1 rounded-xl border border-surface/40 bg-mantle/60 p-1 md:hidden"
			>
				{#snippet children()}
					<ToggleGroupItem
						value="vertical"
						title="Paginado vertical"
						class="h-9 gap-2 rounded-lg text-[10px] font-black tracking-widest uppercase data-[state=on]:bg-primary data-[state=on]:text-primary-foreground"
					>
						<Rows2 size={15} />
						<span>Vertical</span>
					</ToggleGroupItem>

					<ToggleGroupItem
						value="horizontal"
						title="Paginado horizontal"
						class="h-9 gap-2 rounded-lg text-[10px] font-black tracking-widest uppercase data-[state=on]:bg-primary data-[state=on]:text-primary-foreground"
					>
						<Columns2 size={15} />
						<span>Horizontal</span>
					</ToggleGroupItem>

					<ToggleGroupItem
						value="webtoon"
						title="Webtoon"
						class="h-9 gap-2 rounded-lg text-[10px] font-black tracking-widest uppercase data-[state=on]:bg-primary data-[state=on]:text-primary-foreground"
					>
						<ScrollText size={15} />
						<span>Webtoon</span>
					</ToggleGroupItem>
				{/snippet}
			</AcerolaToggleGroup>
		</div>
	</footer>

	{#if commandOpen}
		<div class="absolute inset-0 z-40">
			<button
				type="button"
				aria-label="Fechar comandos"
				class="absolute inset-0 bg-base/20 backdrop-blur-[2px]"
				onclick={() => (commandOpen = false)}
			></button>

			<div
				class="absolute top-1/2 left-1/2 w-[min(34rem,calc(100vw-2rem))] -translate-x-1/2 -translate-y-1/2 overflow-hidden rounded-2xl border border-surface/50 bg-base/95 shadow-2xl shadow-crust/60"
			>
				<AcerolaCommand bind:value={commandValue}>
					{#snippet children()}
						<Command.Input placeholder="Comandos do leitor..." autofocus />

						<Command.List class="p-1">
							<Command.Group heading="Zoom">
								<Command.Item
									value="alternar modo zoom"
									class="cursor-pointer"
									onSelect={() => runCommand(toggleZoomMode)}
								>
									<ZoomIn size={16} />
									<span>{zoomMode ? 'Desativar modo zoom' : 'Ativar modo zoom'}</span>
									<Command.Shortcut>Z</Command.Shortcut>
								</Command.Item>

								<Command.Item
									value="aumentar zoom"
									class="cursor-pointer"
									onSelect={() => runCommand(zoomIn)}
								>
									<ZoomIn size={16} />
									<span>Aumentar zoom</span>
									<Command.Shortcut>Ctrl +</Command.Shortcut>
								</Command.Item>

								<Command.Item
									value="reduzir zoom"
									class="cursor-pointer"
									onSelect={() => runCommand(zoomOut)}
								>
									<ZoomOut size={16} />
									<span>Reduzir zoom</span>
									<Command.Shortcut>Ctrl -</Command.Shortcut>
								</Command.Item>

								<Command.Item
									value="resetar zoom"
									class="cursor-pointer"
									onSelect={() => runCommand(resetZoom)}
								>
									<ZoomOut size={16} />
									<span>Resetar zoom</span>
									<Command.Shortcut>Ctrl 0</Command.Shortcut>
								</Command.Item>
							</Command.Group>

							<Command.Group heading="Leitura">
								<Command.Item
									value="paginado vertical"
									class="cursor-pointer"
									onSelect={() => runCommand(() => (readingMode = 'vertical'))}
								>
									<Rows2 size={16} />
									<span>Paginado vertical</span>
								</Command.Item>

								<Command.Item
									value="paginado horizontal"
									class="cursor-pointer"
									onSelect={() => runCommand(() => (readingMode = 'horizontal'))}
								>
									<Columns2 size={16} />
									<span>Paginado horizontal</span>
								</Command.Item>

								<Command.Item
									value="webtoon"
									class="cursor-pointer"
									onSelect={() => runCommand(() => (readingMode = 'webtoon'))}
								>
									<ScrollText size={16} />
									<span>Webtoon</span>
								</Command.Item>
							</Command.Group>
						</Command.List>
					{/snippet}
				</AcerolaCommand>
			</div>
		</div>
	{/if}
</div>
