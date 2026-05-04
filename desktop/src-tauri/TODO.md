# Migração para acerola-p2p

Este documento detalha o plano de ação para migrar a infraestrutura de rede P2P interna do `src-tauri` para a nova biblioteca central `acerola-p2p`.

## 🗑️ O que será removido do `src-tauri`

A extração para a `acerola-p2p` vai limpar muito o código principal, removendo arquivos de infraestrutura e gerenciamento que agora pertencem à biblioteca.

Arquivos e módulos que serão deletados:
- `src-tauri/src/core/connection/p2p/network_manager.rs` (Gerenciador de conexões Iroh/QUIC e multiplexação)
- `src-tauri/src/core/connection/p2p/state/network_state.rs` (Estado gerenciado e lista de peers)
- `src-tauri/src/data/remote/p2p/iroh_transport.rs` (Implementação do transporte Iroh)
- Traits de base como `infra/remote/p2p/transport.rs` e `infra/remote/p2p/protocol_handler.rs` (substituído pelo `Handler` da lib)
- Os handlers RPC embutidos (`rpc.rs`) que agora são automáticos na lib.
- O boilerplate gigantesco de inicialização e canais `mpsc` dentro do `lib.rs`.

---

## 🛠️ Plano de Implementação

### 1. Atualizar requisitos na `acerola-p2p` (Gap API)
Antes de integrar no Tauri, a biblioteca `acerola-p2p` precisa expor algumas funcionalidades que atualmente são dinâmicas no projeto. É necessário adicionar na lib:

- [ ] **`node.connected_peers()`**: O método `get_network_status` no `network_cmd.rs` precisa repassar a lista de conexões ativas para o front-end. O `AcerolaP2P` deve expor uma função que retorne os peers conectados e seus respectivos ALPNs (ex: `HashMap<String, Vec<u8>>`).
- [ ] **`node.set_guard()` dinâmico**: Os comandos Tauri `switch_to_local` e `switch_to_relay` alteram o validador de conexões em tempo de execução. O `AcerolaP2P` precisa expor algo como `pub async fn set_guard(&self, new_guard: Guard)` para permitir essa troca sem precisar derrubar e recriar o nó.

### 2. Refatorar o `NetworkService` (`src-tauri/src/core/services/network/network_service.rs`)
O serviço de rede deixará de manipular canais de mensagens (`mpsc`) e estado bruto. Ele atuará como um wrapper limpo sobre a `acerola-p2p`.

- [ ] Remover as dependências antigas de `NetworkManager`, `P2PTransport` e `RwLock<NetworkState>`.
- [ ] Injetar uma instância encapsulada `Arc<AcerolaP2P>`.
- [ ] Armazenar internamente o estado simples do modo da rede (`NetworkMode::Local` / `NetworkMode::Relay`) para sincronia com a UI.
- [ ] Repassar chamadas como `connect`, `switch_to_local` (usando `set_guard`) e `connected_peers` diretamente para a instância da lib.

### 3. Refatorar a Inicialização no `lib.rs` (`src-tauri/src/lib.rs`)
A complexidade de montar o nó e registrar eventos vai desaparecer do *app bootstrap*.

- [ ] Remover a instanciação de canais `mpsc`, `IrohTransport`, registro de `RpcServerHandler` e `RpcClientHandler`.
- [ ] Configurar o `EventEmitter` da `acerola-p2p` para fazer ponte com `app.emit`.
- [ ] Utilizar o `AcerolaP2P::builder(emit)` injetando o guard inicial (`open_guard`).
- [ ] Instanciar o `NetworkService` passando o nó construído e registrar como *managed state* do Tauri (`handle.manage(service)`).

### 4. Ajustar os Comandos Tauri (`src-tauri/src/cmd/features/network/network_cmd.rs`)
Como o `NetworkService` vai ficar mais direto, os comandos continuarão os mesmos, mas as assinaturas vão precisar se adequar.

- [ ] Revisar conversões dos tipos de ID (se a lib agora usa puro `String` no lugar da antiga struct `PeerId`).
- [ ] Adaptar o `get_network_status` para casar o modelo novo que virá do `AcerolaP2P`.
