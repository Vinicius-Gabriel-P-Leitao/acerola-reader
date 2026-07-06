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
	import { useComicSummary } from '$lib/hooks/store/use-comic-summary.svelte';
	import { useSelectFolder } from '$lib/hooks/store/use-select-folder.svelte';
	import { setComicContext } from '$lib/state/comic-context.svelte';
	import { getLocale, setLocale } from '$lib/paraglide/runtime';
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { resolveCover } from '$lib/utils/artwork.utils';
	
	import AcerolaModePicker from '$lib/components/acerola-mode-picker/acerola-mode-picker.svelte';
	import AcerolaSelect from '$lib/components/acerola-select/acerola-select.svelte';
	import { globalSearch } from '$lib/state/search.svelte';
	import AcerolaSidebar from '$lib/components/acerola-sidebar/acerola-sidebar.svelte';
	import AcerolaSonner from '$lib/components/acerola-sonner/acerola-sonner.svelte';
	import SidebarProvider from '$lib/components/ui/sidebar/sidebar-provider.svelte';
	import AcerolaDialog from '$lib/components/acerola-dialog/acerola-dialog.svelte';
	import AcerolaCommand from '$lib/components/acerola-command/acerola-command.svelte';
	import * as Command from '$lib/components/ui/command/index.js';
	import { Command as CommandPrimitive } from 'bits-ui';

	import AcerolaNotification from '$lib/components/acerola-notification/acerola-notification.svelte';
	import '$theme/layout.css';
	import Search from '@lucide/svelte/icons/search';

	setComicContext();

	let currentLocale = $state(getLocale());
	let appWindow = $state<any>(null);
	let packageIdentity = $state<string | null>(null);
	let isSearchDialogOpen = $state(false);

	const folder = useSelectFolder();
	const summary = useComicSummary();

	const incrementalScanner = useLibraryScanner(
		DIRECTORY_SCAN_COMMANDS.incrementalScan,
		() => folder.folderPath
	);

	onMount(async () => {
		// INFO: Importação dinâmica para evitar que quebre durante o SSR (Server-Side Rendering)
		const { getCurrentWindow } = await import('@tauri-apps/api/window');
		const { invoke } = await import('@tauri-apps/api/core');
		const { error: logError } = await import('@tauri-apps/plugin-log');
		
		await folder.loadSavedPath();

		try {
			const packageFamilyName = await invoke<string>('get_package_family_name');
			if (packageFamilyName !== "No package identity" && !packageFamilyName.startsWith("Error")) {
				packageIdentity = `Package: ${packageFamilyName}`;
			} else {
				packageIdentity = `Not running with package identity`;
			}
		} catch (error) {
			logError(`Failed to check package identity: ${error}`);
		}

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
					<div class="max-w-xl flex-1">
						{#if $page.url.pathname === '/home'}
							<!-- In-place search for Home -->
							<div class="group relative">
								<Search
									class="text-overlay absolute top-1/2 left-4 -translate-y-1/2 transition-colors group-focus-within:text-primary"
									size={20}
								/>
								<input
									type="text"
									bind:value={globalSearch.query}
									placeholder={m['layout.search_placeholder']()}
									class="placeholder:text-overlay/50 w-full rounded-2xl border border-surface bg-mantle py-3 pr-4 pl-12 transition-all focus:border-primary focus:ring-2 focus:ring-primary/50 focus:outline-none"
								/>
							</div>
						{:else}
							<!-- Trigger for Command Dialog -->
							<button
								class="group relative w-full cursor-text text-left"
								onclick={() => {
									isSearchDialogOpen = true;
									if (!summary.comics) summary.fetch();
								}}
							>
								<Search
									class="text-overlay absolute top-1/2 left-4 -translate-y-1/2 transition-colors group-hover:text-primary"
									size={20}
								/>
								<div
									class="text-overlay/50 flex w-full items-center rounded-2xl border border-surface bg-mantle py-3 pr-4 pl-12 transition-all group-hover:border-primary"
								>
									{m['layout.search_placeholder']()}
								</div>
							</button>
						{/if}
					</div>

					<div class="mx-8 flex items-center gap-4">
						{#if packageIdentity}
							<span class="text-xs text-muted-foreground">{packageIdentity}</span>
						{/if}
						<AcerolaNotification />
					</div>
				</header>

				{@render children()}
			</main>
		</SidebarProvider>
	</div>
</div>

<AcerolaDialog
	state={{ open: isSearchDialogOpen }}
	events={{ onOpenChange: (open) => (isSearchDialogOpen = open) }}
	ui={{ contentClass: 'p-0 border border-surface/50 overflow-hidden w-full max-w-4xl sm:max-w-4xl bg-base/95 backdrop-blur-xl shadow-2xl', showCloseButton: false }}
>
	<AcerolaCommand>
		<div class="border-b border-surface/50 p-4">
			<div class="group relative">
				<Search
					class="text-overlay absolute top-1/2 left-4 -translate-y-1/2 transition-colors group-focus-within:text-primary"
					size={24}
				/>
				<CommandPrimitive.Input
					placeholder={m['layout.search_placeholder']()}
					class="placeholder:text-overlay/50 h-16 w-full rounded-2xl border border-surface bg-mantle py-3 pr-4 pl-14 text-xl font-medium outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/50"
				/>
			</div>
		</div>
		<Command.List class="max-h-[70vh] overflow-y-auto p-4">
			<Command.Empty class="py-24 text-center text-lg text-muted-foreground">
				Nenhum quadrinho encontrado.
			</Command.Empty>
			{#if summary.comics && summary.comics.total > 0}
				<Command.Group heading="Biblioteca" class="text-muted-foreground px-2">
					<div class="flex flex-col gap-2">
						{#each summary.comics.comics as comic (comic.relations.directoryId)}
							{@const cover = resolveCover(comic.artwork)}
							<Command.Item
								onSelect={() => {
									isSearchDialogOpen = false;
									import('$app/navigation').then((n) => n.goto(`/comic/${comic.filesystem.folderName}`));
								}}
								class="flex cursor-pointer items-center gap-6 rounded-2xl px-4 py-4 transition-colors data-[selected=true]:bg-surface/50"
							>
								<div class="h-32 w-24 shrink-0 overflow-hidden rounded-xl border border-surface/50 bg-mantle shadow-sm">
									{#if cover}
										<img src={cover} alt="" class="h-full w-full object-cover" />
									{:else}
										<div class="flex h-full w-full items-center justify-center text-muted-foreground/50">
											<BookOpenIcon size={24} />
										</div>
									{/if}
								</div>
								<div class="flex flex-col gap-2 overflow-hidden">
									<span class="truncate text-xl font-bold text-foreground">
										{comic.metadata.title ?? comic.filesystem.folderName}
									</span>
									<span class="truncate text-sm font-medium text-muted-foreground">
										{comic.filesystem.folderName}
									</span>
								</div>
							</Command.Item>
						{/each}
					</div>
				</Command.Group>
			{/if}
		</Command.List>
	</AcerolaCommand>
</AcerolaDialog>
