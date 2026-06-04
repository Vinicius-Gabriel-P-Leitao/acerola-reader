<script module lang="ts">
	import type { ReaderMode } from '../hooks/use-reader-zoom.svelte';

	export type ReaderCommandPaletteProps = {
		open?: boolean;
		value?: string;
		readingMode?: ReaderMode;
		zoomMode: boolean;
		onToggleZoomMode: () => void;
		onZoomIn: () => void;
		onZoomOut: () => void;
		onResetZoom: () => void;
	};
</script>

<script lang="ts">
	import AcerolaCommand from '$lib/components/acerola-command/acerola-command.svelte';
	import * as Command from '$lib/components/ui/command';
	import { m } from '$lib/paraglide/messages';
	import Columns2 from '@lucide/svelte/icons/columns-2';
	import Rows2 from '@lucide/svelte/icons/rows-2';
	import ScrollText from '@lucide/svelte/icons/scroll-text';
	import ZoomIn from '@lucide/svelte/icons/zoom-in';
	import ZoomOut from '@lucide/svelte/icons/zoom-out';

	let {
		open = $bindable(false),
		value = $bindable(''),
		readingMode = $bindable<ReaderMode>('vertical'),
		zoomMode,
		onToggleZoomMode,
		onZoomIn,
		onZoomOut,
		onResetZoom
	}: ReaderCommandPaletteProps = $props();

	function closeCommand() {
		open = false;
		value = '';
	}

	function runCommand(action: () => void) {
		action();
		closeCommand();
	}
</script>

{#if open}
	<div class="absolute inset-0 z-40">
		<button
			type="button"
			aria-label={m['pages.reader.actions.close_commands']()}
			class="absolute inset-0 bg-base/20 backdrop-blur-[2px]"
			onclick={closeCommand}
		></button>

		<div
			class="absolute top-1/2 left-1/2 w-[min(34rem,calc(100vw-2rem))] -translate-x-1/2 -translate-y-1/2 overflow-hidden rounded-2xl border border-surface/50 bg-base/95 shadow-2xl shadow-crust/60"
		>
			<AcerolaCommand bind:value>
				{#snippet children()}
					<Command.Input placeholder={m['pages.reader.command.placeholder']()} autofocus />

					<Command.List class="p-1">
						<Command.Group heading={m['pages.reader.command.zoom_group']()}>
							<Command.Item
								value={m['pages.reader.command.toggle_zoom_search']()}
								class="cursor-pointer"
								onSelect={() => runCommand(onToggleZoomMode)}
							>
								<ZoomIn size={16} />
								<span>
									{zoomMode
										? m['pages.reader.command.disable_zoom_mode']()
										: m['pages.reader.command.enable_zoom_mode']()}
								</span>
								<Command.Shortcut>Z</Command.Shortcut>
							</Command.Item>

							<Command.Item
								value={m['pages.reader.command.zoom_in_search']()}
								class="cursor-pointer"
								onSelect={() => runCommand(onZoomIn)}
							>
								<ZoomIn size={16} />
								<span>{m['pages.reader.command.zoom_in']()}</span>
								<Command.Shortcut>Ctrl +</Command.Shortcut>
							</Command.Item>

							<Command.Item
								value={m['pages.reader.command.zoom_out_search']()}
								class="cursor-pointer"
								onSelect={() => runCommand(onZoomOut)}
							>
								<ZoomOut size={16} />
								<span>{m['pages.reader.command.zoom_out']()}</span>
								<Command.Shortcut>Ctrl -</Command.Shortcut>
							</Command.Item>

							<Command.Item
								value={m['pages.reader.command.reset_zoom_search']()}
								class="cursor-pointer"
								onSelect={() => runCommand(onResetZoom)}
							>
								<ZoomOut size={16} />
								<span>{m['pages.reader.command.reset_zoom']()}</span>
								<Command.Shortcut>Ctrl 0</Command.Shortcut>
							</Command.Item>
						</Command.Group>

						<Command.Group heading={m['pages.reader.command.reading_group']()}>
							<Command.Item
								value={m['pages.reader.command.vertical_search']()}
								class="cursor-pointer"
								onSelect={() => runCommand(() => (readingMode = 'vertical'))}
							>
								<Rows2 size={16} />
								<span>{m['pages.reader.modes.vertical']()}</span>
							</Command.Item>

							<Command.Item
								value={m['pages.reader.command.horizontal_search']()}
								class="cursor-pointer"
								onSelect={() => runCommand(() => (readingMode = 'horizontal'))}
							>
								<Columns2 size={16} />
								<span>{m['pages.reader.modes.horizontal']()}</span>
							</Command.Item>

							<Command.Item
								value={m['pages.reader.command.webtoon_search']()}
								class="cursor-pointer"
								onSelect={() => runCommand(() => (readingMode = 'webtoon'))}
							>
								<ScrollText size={16} />
								<span>{m['pages.reader.modes.webtoon']()}</span>
							</Command.Item>
						</Command.Group>
					</Command.List>
				{/snippet}
			</AcerolaCommand>
		</div>
	</div>
{/if}
