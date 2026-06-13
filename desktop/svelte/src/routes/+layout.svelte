<script lang="ts" module>
	import type { AcerolaSelectOption } from '$lib/components/acerola-select/acerola-select.svelte';
	import type { SidebarItem } from '$lib/components/acerola-sidebar/acerola-sidebar.svelte';
	import type { Locale } from '$lib/paraglide/runtime.js';

	import { m } from '$lib/paraglide/messages';
	import { locales } from '$lib/paraglide/runtime.js';

	import HistoryIcon from '@lucide/svelte/icons/history';
	import HouseIcon from '@lucide/svelte/icons/house';
	import SettingsIcon from '@lucide/svelte/icons/settings';

	import BookOpenIcon from '@lucide/svelte/icons/book-open';
	import MinusIcon from '@lucide/svelte/icons/minus';
	import SquareIcon from '@lucide/svelte/icons/square';
	import XIcon from '@lucide/svelte/icons/x';

	const localeLabels: Record<string, string> = {
		'pt-br': 'Português',
		en: 'English'
	};

	const localeOptions: AcerolaSelectOption[] = locales.map((locale) => ({
		value: locale,
		label: localeLabels[locale] || locale.toUpperCase()
	}));

	const sidebarItems: SidebarItem[] = $derived([
		{ label: m['routes.home'](), href: '/home', icon: HouseIcon },
		{ label: m['routes.history'](), href: '/history', icon: HistoryIcon },
		{ label: m['routes.config'](), href: '/config', icon: SettingsIcon }
	]);
</script>

<script lang="ts">
	import { DIRECTORY_SCAN_COMMANDS } from '$lib/contracts/library/library.commands';
	import { useLibraryScanner } from '$lib/hooks/store/use-comic-scanner.svelte';
	import { useSelectFolder } from '$lib/hooks/store/use-select-folder.svelte';
	import { setComicContext } from '$lib/state/comic-context.svelte';
	import { getLocale, setLocale } from '$lib/paraglide/runtime';
	import { onMount } from 'svelte';

	import AcerolaModePicker from '$lib/components/acerola-mode-picker/acerola-mode-picker.svelte';
	import AcerolaSelect from '$lib/components/acerola-select/acerola-select.svelte';
	import AcerolaSidebar from '$lib/components/acerola-sidebar/acerola-sidebar.svelte';
	import AcerolaSonner from '$lib/components/acerola-sonner/acerola-sonner.svelte';
	import SidebarProvider from '$lib/components/ui/sidebar/sidebar-provider.svelte';

	import AcerolaNotification from '$lib/components/acerola-notification/acerola-notification.svelte';
	import '$theme/layout.css';
	import Search from '@lucide/svelte/icons/search';

	setComicContext();

	let currentLocale = $state(getLocale());
	let appWindow = $state<any>(null);
	const folder = useSelectFolder();

	const incrementalScanner = useLibraryScanner(
		DIRECTORY_SCAN_COMMANDS.incrementalScan,
		() => folder.folderPath
	);

	onMount(async () => {
		// INFO: Importação dinâmica para evitar que quebre durante o SSR (Server-Side Rendering)
		const { getCurrentWindow } = await import('@tauri-apps/api/window');
		await folder.loadSavedPath();

		appWindow = getCurrentWindow();

		if (folder.folderPath) {
			incrementalScanner.start();
		}
	});

	function minimize() {
		appWindow?.minimize();
	}

	function toggleMaximize() {
		appWindow?.toggleMaximize();
	}

	function closeWindow() {
		appWindow?.close();
	}

	$effect(() => {
		setLocale(currentLocale as Locale);
	});

	const { children } = $props();
</script>

<div class="flex h-screen w-full flex-col overflow-hidden">
	<!-- Titlebar Global -->
	<div
		data-tauri-drag-region
		class="z-100 flex h-8 shrink-0 items-center justify-between border-b border-border/50 bg-background select-none"
	>
		<div
			data-tauri-drag-region
			class="pointer-events-none flex h-full flex-1 items-center pl-4 text-xs font-semibold text-muted-foreground"
		>
			Acerola
		</div>

		<div class="flex h-full text-foreground">
			<button
				class="inline-flex h-full w-11.5 cursor-pointer items-center justify-center transition-colors hover:bg-muted"
				aria-label={m['layout.buttons.minimize']()}
				onclick={minimize}
			>
				<MinusIcon size={16} strokeWidth={1.5} />
			</button>

			<button
				class="inline-flex h-full w-11.5 cursor-pointer items-center justify-center transition-colors hover:bg-muted"
				aria-label={m['layout.buttons.maximize']()}
				onclick={toggleMaximize}
			>
				<SquareIcon size={14} strokeWidth={1.5} />
			</button>

			<button
				class="hover:text-destructive-foreground inline-flex h-full w-11.5 cursor-pointer items-center justify-center transition-colors hover:bg-destructive"
				aria-label={m['layout.buttons.close']()}
				onclick={closeWindow}
			>
				<XIcon size={18} strokeWidth={1.5} />
			</button>
		</div>
	</div>

	<!-- Conteúdo Principal da Aplicação -->
	<div class="relative flex flex-1 overflow-hidden">
		<SidebarProvider class="h-full min-h-0">
			<AcerolaSonner />

			<AcerolaSidebar data={{ items: sidebarItems }} ui={{ class: 'absolute h-full' }}>
				{#snippet header()}
					<div class="flex items-center gap-3">
						<div
							class="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary text-primary-foreground"
						>
							<BookOpenIcon size={24} />
						</div>

						<span class="text-xl font-bold tracking-tight group-data-[collapsible=icon]:hidden">
							Acerola
						</span>
					</div>
				{/snippet}

				{#snippet footer()}
					<div class="flex w-full items-center gap-2 overflow-hidden px-2 pb-2">
						<AcerolaModePicker />

						<AcerolaSelect
							data={{ options: localeOptions }}
							state={{ value: currentLocale }}
							events={{ onValueChange: (value) => (currentLocale = value as Locale) }}
							ui={{ class: 'min-w-0 flex-1' }}
						/>
					</div>
				{/snippet}
			</AcerolaSidebar>

			<main class="flex-1 overflow-y-auto">
				<header
					class="sticky top-0 z-10 flex h-20 items-center justify-between border-b border-surface/50 bg-base/80 px-8 backdrop-blur-xl"
				>
					<!-- FIXME: Fazer o search ser um componente -->
					<div class="max-w-xl flex-1">
						<div class="group relative">
							<Search
								class="text-overlay absolute top-1/2 left-4 -translate-y-1/2 transition-colors group-focus-within:text-primary"
								size={20}
							/>
							<input
								type="text"
								placeholder={m['layout.search_placeholder']()}
								class="placeholder:text-overlay/50 w-full rounded-2xl border border-surface bg-mantle py-3 pr-4 pl-12 transition-all focus:border-primary focus:ring-2 focus:ring-primary/50 focus:outline-none"
							/>
						</div>
					</div>

					<div class="mx-8a flex items-center gap-4">
						<AcerolaNotification />
					</div>
				</header>

				{@render children()}
			</main>
		</SidebarProvider>
	</div>
</div>
