<script lang="ts">
	import { invoke } from '@tauri-apps/api/core';
	import { listen } from '@tauri-apps/api/event';
	import { onDestroy, onMount } from 'svelte';

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
			await listen<string>('rpc:ping_sent', (e) => addLog(e.payload, 'Ping enviado')),
			await listen<string>('rpc:ping_received', (e) => addLog(e.payload, 'Ping recebido')),
			await listen<string>('rpc:pong_sent', (e) => addLog(e.payload, 'Pong enviado')),
			await listen<string>('rpc:pong_received', (e) => addLog(e.payload, 'Pong recebido')),
			await listen<string>('rpc:device_info_exchanged', (e) => {
				addLog(e.payload, 'Info trocada');
				refresh();
			}),
			await listen<string>('rpc:device_info_received', (e) => {
				addLog(e.payload, 'Info recebida');
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
		<h2 class="text-lg font-semibold">Rede P2P</h2>
		<span class="font-mono text-xs break-all text-muted-foreground"
			>ID local: {localId || '...'}</span
		>
	</div>

	<div class="flex items-center gap-4">
		<span class="text-sm text-muted-foreground">Modo: {status?.mode ?? '...'}</span>
		<button onclick={switchLocal} class="text-sm underline">Local</button>
		<button onclick={switchRelay} class="text-sm underline">Relay</button>
	</div>

	<div class="flex gap-2">
		<input
			class="flex-1 rounded border px-2 py-1 text-sm"
			placeholder="Peer ID"
			bind:value={targetPeerId}
		/>
		<button onclick={connect} class="rounded border px-3 py-1 text-sm">Conectar</button>
	</div>

	<div>
		<p class="mb-2 text-sm font-medium">
			Conexões ativas ({status?.peers.length ?? 0})
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
						<span class="text-xs text-muted-foreground italic">Dispositivo desconhecido</span>
					{/if}
				</div>
			{/each}
		{:else}
			<p class="text-sm text-muted-foreground">Nenhuma conexão ativa.</p>
		{/if}
	</div>

	<div>
		<p class="mb-2 text-sm font-medium">Log RPC</p>
		{#if rpcLog.length}
			{#each rpcLog as entry}
				<div class="font-mono text-xs text-muted-foreground">
					<span class="text-foreground">{entry.event}</span> — {entry.peerId}
				</div>
			{/each}
		{:else}
			<p class="text-sm text-muted-foreground">Nenhum evento ainda.</p>
		{/if}
	</div>
</div>
