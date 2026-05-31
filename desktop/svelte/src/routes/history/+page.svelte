<script lang="ts">
	import { invoke } from '@tauri-apps/api/core';
	import { listen } from '@tauri-apps/api/event';
	import { onDestroy, onMount } from 'svelte';
	import { m } from '$lib/paraglide/messages';

	type DeviceInfo = { name: string; os: string; version: string };
	type NetworkConnection = {
		peerId: string;
		alpn: string;
		device: DeviceInfo | null;
	};
	type NetworkStatus = { mode: string; peers: NetworkConnection[] };
	type RpcEvent = { peerId: string; event: string };

	let status: NetworkStatus | null = $state(null);
	let localId = $state('');
	let targetPeerId = $state('');
	let rpcLog = $state<RpcEvent[]>([]);

	let unlisteners: (() => void)[] = [];

	onMount(async () => {
		unlisteners.push(
			await listen<NetworkStatus>('network:status', (e) => {
				status = e.payload;
			}),
			await listen<string>('rpc:ping_sent', (e) => addLog(e.payload, m['pages.history.logs.ping_sent']())),
			await listen<string>('rpc:ping_received', (e) => addLog(e.payload, m['pages.history.logs.ping_received']())),
			await listen<string>('rpc:pong_sent', (e) => addLog(e.payload, m['pages.history.logs.pong_sent']())),
			await listen<string>('rpc:pong_received', (e) => addLog(e.payload, m['pages.history.logs.pong_received']())),
			await listen<string>('rpc:device_info_exchanged', (e) => {
				addLog(e.payload, m['pages.history.logs.info_exchanged']());
				refresh();
			}),
			await listen<string>('rpc:device_info_received', (e) => {
				addLog(e.payload, m['pages.history.logs.info_received']());
				refresh();
			})
		);

		localId = await invoke('get_local_id');
		await invoke('get_network_status');
	});

	onDestroy(() => unlisteners.forEach((u) => u()));

	function addLog(peerId: string, event: string) {
		rpcLog = [{ peerId, event }, ...rpcLog].slice(0, 50);
	}

	async function refresh() {
		await invoke('get_network_status');
	}

	async function switchLocal() {
		await invoke('switch_to_local');
		await refresh();
	}

	async function switchRelay() {
		await invoke('switch_to_relay');
		await refresh();
	}

	async function connect() {
		if (!targetPeerId.trim()) return;
		await invoke('connect_to_peer', {
			peerId: targetPeerId,
			alpn: 'acerola/handshake/1'
		});
		await refresh();
	}
</script>

<div class="flex flex-col gap-6 p-8">
	<div class="flex flex-col gap-1">
		<h2 class="text-lg font-semibold">{m['pages.history.title']()}</h2>
		<span class="font-mono text-xs break-all text-muted-foreground"
			>{m['pages.history.local_id']({ id: localId || '...' })}</span
		>
	</div>

	<div class="flex items-center gap-4">
		<span class="text-sm text-muted-foreground">{m['pages.history.mode']({ mode: status?.mode ?? '...' })}</span>
		<button onclick={switchLocal} class="text-sm underline">{m['pages.history.local']()}</button>
		<button onclick={switchRelay} class="text-sm underline">{m['pages.history.relay']()}</button>
	</div>

	<div class="flex gap-2">
		<input
			class="flex-1 rounded border px-2 py-1 text-sm"
			placeholder="Peer ID"
			bind:value={targetPeerId}
		/>
		<button onclick={connect} class="rounded border px-3 py-1 text-sm">{m['pages.history.connect']()}</button>
	</div>

	<div>
		<p class="mb-2 text-sm font-medium">
			{m['pages.history.active_connections']({ count: status?.peers.length ?? 0 })}
		</p>
		{#if status?.peers.length}
			{#each status.peers as conn}
				<div class="mb-2 flex flex-col gap-1 rounded border p-3">
					<span class="font-mono text-sm break-all">{conn.peerId}</span>
					<span class="text-xs text-muted-foreground">ALPN: {conn.alpn}</span>
					{#if conn.device}
						<div class="mt-1 flex gap-3 text-xs text-muted-foreground">
							<span>{conn.device.name}</span>
							<span>{conn.device.os}</span>
							<span>v{conn.device.version}</span>
						</div>
					{:else}
						<span class="text-xs text-muted-foreground italic">{m['pages.history.unknown_device']()}</span>
					{/if}
				</div>
			{/each}
		{:else}
			<p class="text-sm text-muted-foreground">{m['pages.history.no_active_connections']()}</p>
		{/if}
	</div>

	<div>
		<p class="mb-2 text-sm font-medium">{m['pages.history.rpc_log']()}</p>
		{#if rpcLog.length}
			{#each rpcLog as entry}
				<div class="font-mono text-xs text-muted-foreground">
					<span class="text-foreground">{entry.event}</span> — {entry.peerId}
				</div>
			{/each}
		{:else}
			<p class="text-sm text-muted-foreground">{m['pages.history.no_events']()}</p>
		{/if}
	</div>
</div>
