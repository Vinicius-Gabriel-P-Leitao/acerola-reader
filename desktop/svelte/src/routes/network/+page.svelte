<script lang="ts">
	import { onMount } from 'svelte';
	import { toast } from 'svelte-sonner';
	import { error } from '@tauri-apps/plugin-log';
	import { invoke } from '@tauri-apps/api/core';
	import { listen, type UnlistenFn } from '@tauri-apps/api/event';
	import QRCode from 'qrcode';

	import { m } from '$lib/paraglide/messages';
	import { Button } from '$lib/components/ui/button';
	import { Input } from '$lib/components/ui/input';

	import { usePeerConnection } from '$lib/hooks/store/use-peer-connection.svelte';
	import { useNetworkSync, type TransferLogEntry } from '$lib/hooks/store/use-network-sync.svelte';
	import { useRelaySettings } from '$lib/hooks/preferences/use-relay-settings.svelte';
	import { NETWORK_COMMANDS } from '$lib/contracts/network/network.commands';
	import { NETWORK_EVENTS } from '$lib/contracts/network/network.events';
	import { InvalidConnectionCodeError, shortId } from '$lib/utils/connection-code.utils';

	import Share2Icon from '@lucide/svelte/icons/share-2';
	import QrCodeIcon from '@lucide/svelte/icons/qr-code';
	import PlugIcon from '@lucide/svelte/icons/plug';
	import UsersIcon from '@lucide/svelte/icons/users';
	import ActivityIcon from '@lucide/svelte/icons/activity';
	import CopyIcon from '@lucide/svelte/icons/copy';
	import HistoryIcon from '@lucide/svelte/icons/history';
	import FolderSyncIcon from '@lucide/svelte/icons/folder-sync';
	import RefreshCwIcon from '@lucide/svelte/icons/refresh-cw';
	import CheckIcon from '@lucide/svelte/icons/check';
	import AlertCircleIcon from '@lucide/svelte/icons/alert-circle';
	import ShieldAlertIcon from '@lucide/svelte/icons/shield-alert';
	import ChevronDownIcon from '@lucide/svelte/icons/chevron-down';
	import XIcon from '@lucide/svelte/icons/x';

	const peers = usePeerConnection();
	const sync = useNetworkSync();
	const relay = useRelaySettings();

	let qrDataUrl = $state<string | undefined>(undefined);
	let qrError = $state(false);
	let showRawCode = $state(false);
	let pasteValue = $state('');
	let connectError = $state<string | undefined>(undefined);
	// Não é um toast: enquanto verdadeiro, a chave mestra que protege identidade/peers/confiança
	// está sem a proteção do keyring do SO (ver `security:keyring_unavailable` no backend,
	// `infra::security::MasterKeySource::FallbackFile`) — precisa ficar visível até o usuário
	// dispensar, não sumir sozinho como um toast.
	let keyringUnavailable = $state(false);
	let keyringWarningDismissed = $state(false);

	onMount(() => {
		let unlistenKeyringWarning: UnlistenFn | undefined;

		(async () => {
			// `setup_network` (backend) roda numa task assíncrona à parte e pode terminar antes
			// desta tela montar — um listener sozinho perderia o evento. Consulta o estado atual
			// sob demanda; o listener abaixo cobre o caso do backend ainda não ter terminado.
			try {
				keyringUnavailable = await invoke<boolean>(NETWORK_COMMANDS.getSecurityStatus);
			} catch (err) {
				error(`failed to query security status: ${err}`);
			}

			unlistenKeyringWarning = await listen(NETWORK_EVENTS.keyringUnavailable, () => {
				keyringUnavailable = true;
			});

			await Promise.all([
				peers.loadLocalInfo(),
				peers.startListening(),
				sync.startListening(),
				relay.loadRelayInfo()
			]);
		})();

		return () => {
			unlistenKeyringWarning?.();
			peers.stopListening();
			sync.stopListening();
		};
	});

	$effect(() => {
		const code = peers.connectionCode();
		if (!code) {
			qrDataUrl = undefined;
			qrError = false;
			return;
		}
		QRCode.toDataURL(code, { margin: 4, width: 280, errorCorrectionLevel: 'L' })
			.then((url) => {
				qrDataUrl = url;
				qrError = false;
			})
			.catch((err) => {
				error(`failed to generate QR code: ${err}`);
				qrDataUrl = undefined;
				qrError = true;
			});
	});

	const uniquePeers = $derived(
		peers.status
			? [...new Map(peers.status.peers.map((peer) => [peer.peerId, peer])).values()]
			: []
	);

	async function copyCode() {
		const code = peers.connectionCode();
		if (!code) return;
		await navigator.clipboard.writeText(code);
		toast.success(m['pages.network.copied']());
	}

	async function copyLocalId() {
		if (!peers.localId) return;
		await navigator.clipboard.writeText(peers.localId);
		toast.success(m['pages.network.copied']());
	}

	async function handleConnect() {
		connectError = undefined;
		try {
			await peers.connectWithCode(pasteValue);
			pasteValue = '';
			toast.success(m['pages.network.connect.success']());
		} catch (error) {
			connectError =
				error instanceof InvalidConnectionCodeError
					? m['pages.network.connect.invalid_code']()
					: m['pages.network.connect.error']();
		}
	}

	function peerAddrFor(peerId: string) {
		return peers.getKnownAddr(peerId);
	}

	function lastSyncedLabel(peerId: string): string {
		const timestamp = sync.lastSyncedAt(peerId);
		if (!timestamp) return m['pages.network.peers.never_synced']();
		return m['pages.network.peers.last_synced']({ when: new Date(timestamp).toLocaleString() });
	}

	async function withSync(action: (peerId: string, addrs: number[]) => Promise<void>, peerId: string) {
		const addrs = peerAddrFor(peerId);
		if (!addrs) return;

		try {
			await action(peerId, addrs);
		} catch (error) {
			toast.error(String(error));
		}
	}

	function describeEntry(entry: TransferLogEntry): string {
		switch (`${entry.kind}:${entry.status}`) {
			case 'history:started':
				return m['pages.network.transfers.history_started']({
					peer: peers.peerLabel(entry.message)
				});
			case 'history:complete':
				return m['pages.network.transfers.history_complete']({
					peer: peers.peerLabel(entry.message)
				});
			case 'history:error':
				return m['pages.network.transfers.history_error']({ msg: entry.message });
			case 'files:started':
				return m['pages.network.transfers.files_started']({
					peer: peers.peerLabel(entry.message)
				});
			case 'files:progress':
				return m['pages.network.transfers.files_progress']({ item: entry.message });
			case 'files:complete':
				return m['pages.network.transfers.files_complete']({
					peer: peers.peerLabel(entry.message)
				});
			case 'files:error':
				return m['pages.network.transfers.files_error']({ msg: entry.message });
			default:
				return entry.message;
		}
	}
</script>

<div class="mx-auto w-full max-w-5xl space-y-12 p-8">
	<!-- Header -->
	<div>
		<h1 class="text-3xl font-bold tracking-tight text-foreground">
			{m['pages.network.title']()}
		</h1>
		<p class="mt-2 text-muted-foreground">
			{m['pages.network.subtitle']()}
		</p>
	</div>

	{#if keyringUnavailable && !keyringWarningDismissed}
		<div class="flex items-start gap-3 rounded-2xl border border-destructive/30 bg-destructive/5 p-4">
			<ShieldAlertIcon size={18} class="mt-0.5 shrink-0 text-destructive" />
			<p class="flex-1 text-sm text-foreground">{m['pages.network.keyring_unavailable']()}</p>
			<button
				type="button"
				class="shrink-0 rounded-md p-1 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
				onclick={() => (keyringWarningDismissed = true)}
				aria-label={m['pages.network.dismiss']()}
			>
				<XIcon size={14} />
			</button>
		</div>
	{/if}

	<!-- Meu dispositivo -->
	<section class="space-y-4">
		<div
			class="flex items-center gap-3 text-xs font-bold tracking-widest text-muted-foreground uppercase"
		>
			<Share2Icon size={16} />
			{m['pages.network.my_device.title']()}
		</div>

		<div class="rounded-2xl border border-border/40 bg-card/50 p-6 backdrop-blur-sm">
			<div class="flex flex-wrap items-center justify-between gap-4">
				<div class="min-w-0">
					<p class="truncate text-sm font-semibold text-foreground">
						{peers.localDeviceInfo?.name ?? '...'}
					</p>
					<p class="mt-1 truncate font-mono text-xs text-muted-foreground">
						{peers.localId ? shortId(peers.localId) : '...'}
					</p>
					<p class="mt-1 text-xs text-muted-foreground">
						{peers.status?.mode === 'relay'
							? m['pages.network.my_device.mode_relay']()
							: m['pages.network.my_device.mode_local']()}
						· {m['pages.network.my_device.relay_label']()}: {relay.relayInfo?.activeRelay ?? '...'}
						{#if relay.isOverridden}
							<span class="text-chart-4">({m['pages.network.my_device.relay_custom']()})</span>
						{:else}
							<span>({m['pages.network.my_device.relay_default']()})</span>
						{/if}
					</p>
				</div>

				<Button
					variant="outline"
					size="sm"
					class="gap-2"
					onclick={copyLocalId}
					disabled={!peers.localId}
				>
					<CopyIcon size={14} />
					{m['pages.network.my_device.copy_id']()}
				</Button>
			</div>
		</div>
	</section>

	<!-- Código de conexão / QR -->
	<section class="space-y-4">
		<div
			class="flex items-center gap-3 text-xs font-bold tracking-widest text-muted-foreground uppercase"
		>
			<QrCodeIcon size={16} />
			{m['pages.network.pairing.title']()}
		</div>

		<div class="rounded-2xl border border-border/40 bg-card/50 p-6 backdrop-blur-sm">
			<p class="mb-4 text-sm text-muted-foreground">{m['pages.network.pairing.desc']()}</p>

			<div class="flex flex-col items-center gap-4">
				<div class="flex h-72 w-72 shrink-0 items-center justify-center rounded-xl bg-white p-3">
					{#if qrDataUrl}
						<img src={qrDataUrl} alt={m['pages.network.pairing.title']()} class="h-full w-full" />
					{:else if qrError}
						<span class="px-4 text-center text-xs text-destructive">
							{m['pages.network.pairing.qr_error']()}
						</span>
					{:else}
						<span class="text-xs text-muted-foreground">{m['pages.network.pairing.loading']()}</span>
					{/if}
				</div>

				<Button
					variant="outline"
					size="sm"
					class="gap-2"
					onclick={copyCode}
					disabled={!peers.connectionCode()}
				>
					<CopyIcon size={14} />
					{m['pages.network.pairing.copy']()}
				</Button>

				<button
					type="button"
					class="flex items-center gap-1 text-xs text-muted-foreground transition-colors hover:text-foreground"
					onclick={() => (showRawCode = !showRawCode)}
				>
					{showRawCode ? m['pages.network.pairing.hide_code']() : m['pages.network.pairing.show_code']()}
					<ChevronDownIcon
						size={12}
						class="transition-transform {showRawCode ? 'rotate-180' : ''}"
					/>
				</button>

				{#if showRawCode}
					<textarea
						readonly
						value={peers.connectionCode() ?? ''}
						class="h-24 w-full resize-none rounded-xl border border-border/60 bg-background/70 p-3 font-mono text-xs break-all text-muted-foreground"
					></textarea>
				{/if}
			</div>

			<div class="mt-6 flex items-start gap-2 rounded-xl border border-chart-4/20 bg-chart-4/5 p-3">
				<ShieldAlertIcon size={16} class="mt-0.5 shrink-0 text-chart-4" />
				<p class="text-xs text-muted-foreground">{m['pages.network.pairing.security_note']()}</p>
			</div>
		</div>
	</section>

	<!-- Conectar via código -->
	<section class="space-y-4">
		<div
			class="flex items-center gap-3 text-xs font-bold tracking-widest text-muted-foreground uppercase"
		>
			<PlugIcon size={16} />
			{m['pages.network.connect.title']()}
		</div>

		<div class="rounded-2xl border border-border/40 bg-card/50 p-6 backdrop-blur-sm">
			<div class="flex flex-col gap-3 sm:flex-row">
				<Input
					bind:value={pasteValue}
					placeholder={m['pages.network.connect.placeholder']()}
					class="h-10 flex-1 bg-background font-mono text-xs"
				/>
				<Button
					class="h-10 gap-2 px-6"
					disabled={!pasteValue.trim() || peers.connecting}
					onclick={handleConnect}
				>
					<PlugIcon size={16} />
					{m['pages.network.connect.button']()}
				</Button>
			</div>

			{#if connectError}
				<p class="mt-3 flex items-center gap-2 text-sm text-destructive">
					<AlertCircleIcon size={14} />
					{connectError}
				</p>
			{/if}
		</div>
	</section>

	<!-- Dispositivos conectados -->
	<section class="space-y-4">
		<div
			class="flex items-center gap-3 text-xs font-bold tracking-widest text-muted-foreground uppercase"
		>
			<UsersIcon size={16} />
			{m['pages.network.peers.title']()}
		</div>

		<div class="grid gap-4">
			{#if uniquePeers.length === 0}
				<div
					class="rounded-xl border border-dashed border-border/40 p-8 text-center text-sm text-muted-foreground"
				>
					{m['pages.network.peers.empty']()}
				</div>
			{:else}
				{#each uniquePeers as peer (peer.peerId)}
					<div
						class="flex flex-col gap-3 rounded-2xl border border-border/40 bg-card/50 p-4 backdrop-blur-sm sm:flex-row sm:items-center sm:justify-between"
					>
						<div class="min-w-0">
							<p class="truncate text-sm font-semibold text-foreground">
								{peer.device?.name ?? shortId(peer.peerId)}
							</p>
							<p class="mt-0.5 truncate font-mono text-xs text-muted-foreground">
								{shortId(peer.peerId)}
							</p>
							<p class="mt-0.5 truncate text-xs text-muted-foreground">
								{lastSyncedLabel(peer.peerId)}
							</p>
						</div>

						<div class="flex shrink-0 gap-2">
							<Button
								variant="outline"
								size="sm"
								class="gap-2"
								disabled={!peerAddrFor(peer.peerId) || sync.isSyncing(peer.peerId, 'history')}
								onclick={() => withSync((id, addrs) => sync.syncHistory(id, addrs), peer.peerId)}
							>
								{#if sync.isSyncing(peer.peerId, 'history')}
									<RefreshCwIcon size={14} class="animate-spin" />
								{:else}
									<HistoryIcon size={14} />
								{/if}
								{m['pages.network.peers.sync_history']()}
							</Button>

							<Button
								variant="outline"
								size="sm"
								class="gap-2"
								disabled={!peerAddrFor(peer.peerId) || sync.isSyncing(peer.peerId, 'files')}
								onclick={() => withSync((id, addrs) => sync.syncFiles(id, addrs), peer.peerId)}
							>
								{#if sync.isSyncing(peer.peerId, 'files')}
									<RefreshCwIcon size={14} class="animate-spin" />
								{:else}
									<FolderSyncIcon size={14} />
								{/if}
								{m['pages.network.peers.sync_files']()}
							</Button>

							<Button
								size="sm"
								class="gap-2"
								disabled={!peerAddrFor(peer.peerId) ||
									sync.isSyncing(peer.peerId, 'history') ||
									sync.isSyncing(peer.peerId, 'files')}
								onclick={() => withSync((id, addrs) => sync.syncAll(id, addrs), peer.peerId)}
							>
								<RefreshCwIcon
									size={14}
									class={sync.isSyncing(peer.peerId, 'history') || sync.isSyncing(peer.peerId, 'files')
										? 'animate-spin'
										: ''}
								/>
								{m['pages.network.peers.sync_all']()}
							</Button>
						</div>
					</div>
				{/each}
			{/if}
		</div>
	</section>

	<!-- Transferências -->
	<section class="space-y-4">
		<div
			class="flex items-center gap-3 text-xs font-bold tracking-widest text-muted-foreground uppercase"
		>
			<ActivityIcon size={16} />
			{m['pages.network.transfers.title']()}
		</div>

		<div class="rounded-2xl border border-border/40 bg-card/50 p-4 backdrop-blur-sm">
			{#if sync.log.length === 0}
				<p class="p-4 text-center text-sm text-muted-foreground">
					{m['pages.network.transfers.empty']()}
				</p>
			{:else}
				<ul class="max-h-80 space-y-1 overflow-y-auto">
					{#each sync.log as entry (entry.id)}
						<li class="flex items-center gap-3 rounded-lg px-3 py-2 text-sm hover:bg-muted/50">
							{#if entry.status === 'error'}
								<AlertCircleIcon size={14} class="shrink-0 text-destructive" />
							{:else if entry.status === 'complete'}
								<CheckIcon size={14} class="shrink-0 text-chart-3" />
							{:else}
								<RefreshCwIcon size={14} class="shrink-0 animate-spin text-muted-foreground" />
							{/if}

							<span class="flex-1 truncate text-foreground">{describeEntry(entry)}</span>
							<span class="shrink-0 text-xs text-muted-foreground">
								{new Date(entry.timestamp).toLocaleTimeString()}
							</span>
						</li>
					{/each}
				</ul>
			{/if}
		</div>
	</section>
</div>
