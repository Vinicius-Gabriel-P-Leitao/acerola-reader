# Refatoração acerola-p2p

## 1. Invocar o validator no loop de accept (`network_manager.rs`)

O `NetworkManager` armazena `validator: Arc<RwLock<BoxedValidator>>` mas nunca o chama no loop de `accept()`. Qualquer peer conecta sem passar pela guarda.

**O que fazer:**
- Chamar `self.validator.read().await` após `incoming.accept_bi()` e antes de invocar o handler
- Construir um `ConnectionContext { peer_id: peer.clone(), data: () }` e passar para o validator
- Se o validator retornar `Err`, logar e encerrar a task sem chamar o handler
- Remover `token_guard` e `peer_guard` — eram stubs sem uso real; o guard agora é injetado pelo consumidor (ver item 5)

---

## 2. Suporte a múltiplos ALPNs por peer no `NetworkState`

`connected_peers: HashMap<PeerId, Vec<u8>>` usa o peer como chave única — a segunda conexão do mesmo peer (em ALPN diferente) sobrescreve a primeira.

**O que fazer:**
- Trocar para `HashMap<PeerId, HashSet<Vec<u8>>>` para manter todos os ALPNs ativos por peer
- Ajustar `connect()` para inserir o ALPN no set existente (ou criar um novo set)
- Ajustar `disconnect()` para receber o ALPN também e remover só aquela entrada; remover o peer do map só quando o set ficar vazio
- Ajustar `is_connected()` — manter semântica atual (peer está conectado em qualquer ALPN)
- Adicionar `is_connected_on(&self, peer, alpn)` para consulta específica

---

## 3. Trocar `unbounded_channel` por `bounded` em `NetworkCommand`

Canal ilimitado não tem backpressure — sob carga o produtor nunca bloqueia e o canal cresce sem limite.

**O que fazer:**
- Trocar `mpsc::unbounded_channel()` por `mpsc::channel(64)` no `NetworkManager::new()`
- Ajustar o tipo de `command_tx` de `UnboundedSender` para `Sender`
- O `send()` passa a ser `.await` — o produtor bloqueia se o canal estiver cheio, o que é o comportamento desejado
- Avaliar se 64 é razoável para o caso de uso (connect/shutdown são pouco frequentes — pode ser menor)

---

## 4. Remover JSON do ping/pong interno (`handlers/rpc.rs`)

`RpcRequest::Ping` e `RpcResponse::Pong` são serializados como JSON — overhead desnecessário para um keepalive interno que carrega zero informação útil.

**O que fazer:**
- Substituir `serde_json` por bytes fixos: `0x01` = Ping, `0x02` = Pong
- Remover `#[derive(Serialize, Deserialize)]` de `RpcRequest` e `RpcResponse`
- Remover `serde_json` das dependências do `handlers/rpc.rs` (se não usado em outro lugar)
- Manter `LengthDelimitedCodec` — o framing ainda é necessário

---

## 5. Guards personalizados injetáveis + remover stubs

O consumidor deve poder definir sua própria lógica de validação de conexão. Manter apenas `open_guard` como default (aceita tudo) — `token_guard` e `peer_guard` eram stubs e serão removidos.

**O que fazer:**
- Manter `BoxedValidator` como tipo público re-exportado pela `api.rs`
- Manter `open_guard` como único guard built-in (default do builder)
- Remover `token_guard` e `peer_guard` de `data/remote/p2p/`
- O builder expõe `.guard(validator: BoxedValidator)` para o consumidor injetar seu próprio guard
- Exemplo de uso pelo consumidor:
  ```rust
  AcerolaP2P::builder(emit)
      .guard(Box::new(|ctx| Box::pin(async move {
          if ctx.peer_id.id == "banned" {
              return Err(ConnectionError::Unauthorized);
          }
          Ok(())
      })))
      .build().await?
  ```
- Se `.guard()` não for chamado, o builder usa `open_guard` automaticamente

---

## 6. API pública (`src/api.rs` + `src/lib.rs`)

Expor uma fachada limpa para que desktop e Android consumam a lib sem conhecer `NetworkManager`, `IrohTransport`, ou handlers internos.

**O que fazer:**
- Criar `src/api.rs` com `AcerolaP2P` e `AcerolaP2PBuilder`
- O builder recebe `EventEmitter` + handlers externos via `.inbound(alpn, handler)` / `.outbound(alpn, handler)`
- Registrar `acerola/rpc` (ping/pong) automaticamente no builder — o consumidor não precisa saber
- Expor: `build() -> AcerolaP2P`, `local_id()`, `connect(peer, alpn)`, `shutdown()`
- Re-exportar no `lib.rs`: `pub use api::AcerolaP2P` e os tipos públicos necessários (`ProtocolHandler`, `PeerId`, `EventEmitter`, `ConnectionError`, `BoxedValidator`)
- Manter `core/`, `infra/`, `data/` como `pub(crate)` — nada vaza além do `api.rs`
