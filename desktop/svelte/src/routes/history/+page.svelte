<script lang="ts">
  import { invoke } from "@tauri-apps/api/core";
  import { toast } from "svelte-sonner";

  let nodeId = "";
  let targetId = "";
  let status = "";

  async function loadNodeId() {
    try {
      nodeId = await invoke("get_node_id");
      status = "NodeId carregado";
    } catch (e) {
      status = "Erro ao pegar NodeId";
      toast.error(String(e));
    }
  }

  async function connect() {
    try {
      status = "Conectando...";
      await invoke("connect_to", { nodeId: targetId });
      status = "Conectado com sucesso";
    } catch (e) {
      status = "Erro ao conectar";
      toast.error(String(e));
    }
  }
</script>

<div style="padding: 16px; font-family: sans-serif;">
  <h2>Iroh Test</h2>

  <button on:click={loadNodeId}>
    Gerar NodeId
  </button>

  {#if nodeId}
    <p><b>Seu NodeId:</b></p>
    <code style="word-break: break-all;">{nodeId}</code>
  {/if}

  <hr />

  <input
    placeholder="Cole o NodeId aqui"
    bind:value={targetId}
    style="width: 100%; margin-top: 8px;"
  />

  <button on:click={connect} style="margin-top: 8px;">
    Conectar
  </button>

  <p style="margin-top: 10px;">{status}</p>
</div>