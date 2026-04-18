<script lang="ts">
  import { invoke } from "@tauri-apps/api/core";
  import { listen } from "@tauri-apps/api/event";
  import { onDestroy, onMount } from "svelte";

  type NetworkConnection = { peerId: string; alpn: string };
  type NetworkStatus = { mode: string; connections: NetworkConnection[] };

  let status: NetworkStatus | null = $state(null);
  let localId = $state("");
  let targetPeerId = $state("");
  let unlisten: (() => void) | undefined;

  onMount(async () => {
    unlisten = await listen<NetworkStatus>("network:status", (event) => {
      status = event.payload;
    });

    localId = await invoke("get_local_id");
    await invoke("get_network_status");
  });

  onDestroy(() => unlisten?.());

  async function refresh() {
    await invoke("get_network_status");
  }

  async function switchLocal() {
    await invoke("switch_to_local");
    await refresh();
  }

  async function switchRelay() {
    await invoke("switch_to_relay");
    await refresh();
  }

  async function connect() {
    if (!targetPeerId.trim()) return;
    await invoke("connect_to_peer", { peerId: targetPeerId, alpn: "acerola/rpc" });
    await refresh();
  }
</script>

<div class="p-8 flex flex-col gap-6">
  <div class="flex flex-col gap-1">
    <h2 class="text-lg font-semibold">Rede P2P</h2>
    <span class="text-xs text-muted-foreground font-mono break-all">ID local: {localId || "..."}</span>
  </div>

  <div class="flex items-center gap-4">
    <span class="text-sm text-muted-foreground">Modo: {status?.mode ?? "..."}</span>
    <button onclick={switchLocal} class="text-sm underline">Local</button>
    <button onclick={switchRelay} class="text-sm underline">Relay</button>
  </div>

  <div class="flex gap-2">
    <input
      class="border rounded px-2 py-1 text-sm flex-1"
      placeholder="Peer ID"
      bind:value={targetPeerId}
    />
    <button onclick={connect} class="border rounded px-3 py-1 text-sm">Conectar</button>
  </div>

  <div>
    <p class="text-sm font-medium mb-2">Conexões ativas ({status?.connections.length ?? 0})</p>
    {#if status?.connections.length}
      {#each status.connections as conn}
        <div class="text-sm font-mono text-muted-foreground">{conn.peerId} — {conn.alpn}</div>
      {/each}
    {:else}
      <p class="text-sm text-muted-foreground">Nenhuma conexão ativa.</p>
    {/if}
  </div>
</div>
