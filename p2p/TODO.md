# AcerolaP2P — Roadmap de Production-Readiness

## Etapa 0 — Correções Críticas (fazer antes de qualquer outra coisa)

- [x] **Fix ALPN registration bug** — `api.rs` + `transport/iroh.rs`
  - `build()` deve coletar todos os ALPNs de `handlers_inbound` e passá-los para `IrohTransport::new(alpns)`
  - Sem esse fix, protocolos customizados registrados via `.inbound()` são silenciosamente rejeitados pelo Iroh no nível TLS
  - Critério: dois nós conseguem trocar mensagens em um ALPN customizado (não-rpc)

- [x] **Relay configurável via builder** — `api.rs` + `transport/iroh.rs`
  - Adicionar `.relay(url: &str)` no `AcerolaP2PBuilder`
  - `IrohTransport::new()` recebe `relay_url: &str` ao invés de hardcoded
  - Remover o FIXME em `iroh.rs:114`
  - Critério: trocar a URL do relay sem recompilar

---

## Etapa 1 — Identidade Determinística

 ❗Mudança de planos, usaremos blake3

- [ ] **Seed → SecretKey via HKDF** — `transport/iroh.rs` + `api.rs`
  - Adicionar dependências: `hkdf`, `sha2`
  - Implementar `derive_secret_key(seed: &[u8; 32]) -> iroh::SecretKey` com HKDF-SHA256
  - Usar `Endpoint::builder().secret_key(key)` para fixar a identidade
  - Expor `.seed(seed: [u8; 32])` no `AcerolaP2PBuilder`
  - Critério: `PeerId` idêntico após reiniciar com a mesma seed

- [ ] **DeviceId determinístico** — `peer.rs`
  - `PeerId` expõe um `device_id: String` derivado do hash Blake3 da chave pública
  - Critério: `device_id` é o mesmo UUID a cada reinício com mesma seed

---

## Etapa 2 — Bootstrap Handshake

- [ ] **Protocolo de apresentação** — `protocol/hello.rs` (novo arquivo)
  - Definir struct `DeviceInfo { name, os, version, public_key }`
  - Serialização via MessagePack (`rmp-serde`)
  - Ao estabelecer qualquer conexão, a primeira troca obrigatória é `DeviceInfo` em ambas as direções
  - `NetworkState` armazena `DeviceInfo` indexado por `PeerId`
  - Critério: logo após conectar, ambos os nós conhecem nome/OS/versão do peer sem segunda requisição

---

## Etapa 3 — Segurança (Challenge-Response)

- [ ] **Desafio criptográfico Ed25519** — `protocol/auth.rs` (novo arquivo)
  - Adicionar dependências: `ed25519-dalek`, `rand`
  - Fluxo: `A → B: Challenge(32 bytes)` → `B → A: Signature(Ed25519)` → A verifica contra NodeId de B
  - Integrar com `ConnectionContext`: adicionar campo `challenge_verified: bool`
  - Guard pode rejeitar conexões com `!ctx.challenge_verified`
  - Critério: nó não-pareado não consegue enviar mensagens de sistema

---

## Etapa 4 — Event Loop Robusto

- [ ] **Reconexão com exponential backoff** — `network.rs`
  - Falhas de `open_bi` disparam retries: 1s → 2s → 4s → 8s → 60s (cap)
  - Máximo de 5 tentativas antes de reportar `PeerDisconnected`
  - Critério: queda de rede de 30s se reconecta automaticamente sem intervenção

- [ ] **Monitoramento de latência** — `network.rs`
  - Task periódica a cada 30s chama `endpoint.latency()` para cada peer conectado
  - Emite evento `"network:latency"` com o valor para a UI
  - Critério: evento de latência disparado a cada ~30s por peer ativo

- [ ] **Mensagem Goodbye no shutdown** — `network.rs` + `protocol/rpc.rs`
  - `NetworkCommand::Shutdown` envia mensagem `0x03 GOODBYE` para todos os peers antes de fechar
  - Peers receptores atualizam status imediatamente
  - Critério: peer remoto detecta desconexão em < 1s após shutdown gracioso

---

## Etapa 5 — Persistência (P2PStorage)

- [ ] **Trait de callback de storage** — `storage.rs` (novo arquivo)
  - Definir `trait P2PStorage: Send + Sync` com métodos async: `save_identity`, `load_identity`, `save_peer`, `load_peers`
  - Separação de responsabilidades: Vault (chaves) vs Cache (peers/endereços)
  - Injetar via `.storage(impl P2PStorage)` no builder
  - Critério: peers descobertos via mDNS são lembrados após restart

- [ ] **Atomicidade na persistência de peers** — `network.rs` + `storage.rs`
  - Se `save_peer` falhar, a conexão é encerrada (não fica em estado inconsistente)
  - Critério: sem peers "fantasma" no estado após falha de storage

---

## Tabela de Verificação Final

| Critério | Etapa |
|---|---|
| Protocolos customizados funcionam (ALPN fix) | 0 |
| Relay próprio configurado | 0 |
| PeerId idêntico após reinstalar (mesma seed) | 1 |
| Nó conectado = UI já sabe nome/OS/versão | 2 |
| Nó não-pareado não envia mensagens de sistema | 3 |
| Reconexão automática em queda de rede | 4 |
| Shutdown gracioso notifica peers em < 1s | 4 |
| Peers lembrados após restart | 5 |
| CPU próximo de 0% em standby | 4 (latência periódica, não polling) |
