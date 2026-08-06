# AcerolaP2P — TODO de Qualidade e Evolução

> Baseado na análise de quality metrics do projeto (2026-08-05).
> Todos os itens das etapas anteriores foram concluídos, exceto os marcados abaixo.

---

## Etapa A — Itens Pendentes do Roadmap Anterior

- [ ] **Implementar UniFFI para DeviceInfo** — `Cargo.toml` + `data/identity/device_info.rs`
  - Adicionar dependência opcional: `uniffi = { version = "...", optional = true }` 
  - Adicionar feature: `[features] uniffi = ["dep:uniffi"]`
  - Adicionar `#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]` na struct `DeviceInfo`
  - Critério: `cargo build --features uniffi` compila; binding gerado expõe `DeviceInfo` como record no Kotlin

---

## Etapa B — Function Size & Cyclomatic Complexity

- [x] **Refatorar `NetworkManager::run()`** — `core/network/manager.rs`
  - Função atual tem ~171 linhas e CC estimada de 18–22 (limite recomendado: ≤10)
  - Extrair bloco de aceite de conexão inbound em `fn handle_incoming(&mut self, incoming: ...) -> ...`
  - Extrair bloco de connect outbound (com retries) em `fn handle_connect_command(&mut self, addr, alpn) -> ...`
  - Extrair bloco de latência periódica em `fn emit_latency_for_all_peers(&self) -> ...`
  - Extrair bloco de `SwitchGuard` em `fn handle_switch_guard(&mut self, validator, mode)`
  - Após refatoração, `run()` tem ~35 linhas ✅
  - Critério: **88/88 testes passando** ✅

- [x] **Simplificar `IrohTransport::latency()`** — `core/transport/iroh/transport.rs`
  - Função tem dupla iteração sobre paths (selected + any) com CC ~8
  - Unificado num único `Iterator::find_map` com `.filter().chain().find_map()` ✅
  - Critério: lógica idêntica, código mais legível e CC ≤ 4 ✅

---

## Etapa C — Code Coverage

- [ ] **Integrar `cargo-llvm-cov` ao CI** — `.github/workflows/`
  - Adicionar step de coverage após `cargo test`:
    ```
    cargo install cargo-llvm-cov
    cargo llvm-cov --all-features --lcov --output-path lcov.info
    ```
  - Configurar upload para Codecov ou similar
  - Definir gate de qualidade mínimo: **80% de line coverage**
  - Critério: PR que derruba cobertura abaixo de 80% falha no CI

- [ ] **Adicionar testes para `core/device/`** — `core/device/android.rs`, `linux.rs`, `windows.rs`
  - Atualmente com 0% de cobertura
  - Escrever testes unitários que verificam se `DefaultDeviceInfoProvider::provide()` retorna `Ok(DeviceInfo)` com campos não-vazios
  - Usar mocking ou test cfg condicional para plataformas não-nativas
  - Critério: cobertura de `core/device/` ≥ 70%

- [ ] **Adicionar testes para `data/identity/device_info.rs`** — `data/identity/device_info.rs`
  - Struct `DeviceInfo` e trait `DeviceInfoProvider` sem testes
  - Testar serialização/deserialização roundtrip com `serde_json`
  - Testar que `DeviceInfo` com campos vazios serializa corretamente
  - Critério: cobertura do módulo ≥ 80%

- [ ] **Adicionar testes para `infra/error/mod.rs`** — `infra/error/mod.rs`
  - Variantes `ConnectionError` e conversões `From<>` sem testes
  - Testar todas as variantes via `Display` e mensagens de erro
  - Testar `From<getrandom::Error>` para `ConnectionError`
  - Critério: cobertura do módulo ≥ 80%

- [ ] **Cobrir caminho `IncomingAddr::Relay` no `accept()`** — `core/transport/iroh/transport.rs`
  - O branch `IncomingAddr::Relay { url, .. }` não tem teste
  - Adicionar teste de integração mockado que simula conexão via relay
  - Critério: branch coverage do método `accept()` ≥ 85%

- [ ] **Cobrir `NetworkManager::run()` com peers conectados no shutdown** — `core/network/manager.rs`
  - Shutdown atual é testado sem peers; o `broadcast_goodbye()` com peers reais não é coberto
  - Adicionar teste que conecta 2+ peers via mock, depois envia `Shutdown` e verifica que o goodbye é emitido
  - Critério: `broadcast_goodbye()` tem ≥ 1 teste com peers presentes

- [ ] **Cobrir `AcerolaP2pBuilder::restore_cached_peers()` com falha de storage** — `api/acerola_builder.rs`
  - Caminho de `load_peers()` retornando `Err(...)` não é testado
  - Adicionar teste com `FailingStorage` que falha em `load_peers`
  - Verificar que o build não panics e continua normalmente (falha silenciosa esperada)
  - Critério: teste cobre o caminho de erro do `restore_cached_peers`

---

## Etapa D — Mutation Testing

- [ ] **Integrar `cargo-mutants` ao CI** — `.github/workflows/`
  - `cargo install cargo-mutants`
  - `cargo mutants --package acerola-p2p` em schedule semanal (não em cada PR — é lento)
  - Score alvo: **≥75% de mutantes mortos**
  - Critério: relatório de mutantes gerado e publicado como artefato de CI

- [ ] **Fortalecer testes do backoff de reconexão** — `core/network/manager.rs`
  - A lógica `backoff * 2` não é verificada quantitativamente pelos testes
  - Adicionar teste que injeta 3+ falhas consecutivas e mede que o tempo de espera cresce exponencialmente
  - Verificar que após 5 tentativas a task termina sem panic
  - Critério: mutação de `backoff * 2` para `backoff + 1` deve ser detectada pelos testes

- [ ] **Fortalecer testes de `resolve_identity()` com seed inválida** — `api/acerola_builder.rs`
  - Caminho onde `load_identity()` retorna bytes com tamanho errado (falha no `try_into()`) não é testado em isolamento
  - Adicionar teste unitário que verifica que a função regenera seed ao receber bytes inválidos do storage
  - Critério: mutação no `try_into()` é detectada

---

## Etapa E — Dependency Structure

- [ ] **Desacoplar `data/protocol/rpc` de `core::network::state`**
  - `rpc/client.rs` e `rpc/server.rs` importam `NetworkState` diretamente — dependência cruzada entre camadas `data` e `core`
  - Criar trait `DeviceInfoStore` em `data/protocol/`:
    ```rust
    pub trait DeviceInfoStore: Send + Sync {
        async fn store_device_info(&self, peer: PeerId, info: DeviceInfo);
    }
    ```
  - Fazer `NetworkState` implementar `DeviceInfoStore`
  - Handlers RPC recebem `Arc<dyn DeviceInfoStore>` ao invés de `Arc<RwLock<NetworkState>>`
  - Critério: `data/protocol/rpc/` não importa mais nada de `core::network`

- [ ] **Mover a constante `GOODBYE` para o `NetworkManager`** — `core/network/manager.rs`
  - `manager.rs` importa `GOODBYE` de `data::protocol::rpc` — acoplamento desnecessário entre rede e protocolo
  - Definir `const GOODBYE: u8 = 0x03` no próprio `manager.rs` (ou em `data/protocol/rpc/mod.rs` com re-export público)
  - Critério: `manager.rs` não importa mais módulos de `data::protocol::rpc`

---

## Etapa F — Qualidade Geral de Código

- [ ] **Trocar `std::sync::Mutex` por `tokio::sync::Mutex` no `TofuGuard`** — `core/guard/tofu.rs`
  - `InMemoryTrustedStore` usa `std::sync::Mutex` dentro de um contexto async
  - Risco: se o lock for mantido através de um `.await` (ex: em futures futuras), ocorre deadlock no tokio runtime
  - Substituir `Mutex<HashSet<String>>` por `tokio::sync::Mutex<HashSet<String>>`
  - Atualizar métodos para `async fn insert`, `async fn contains`, `async fn is_blocked`
  - Critério: `TofuGuard` funciona sem risco de bloquear o runtime

- [ ] **Substituir `expect()` em código de produção** — `core/transport/iroh/transport.rs:49`
  - `serde_json::to_vec(&addr).expect("EndpointAddr serialization failed")` causa panic em produção
  - Propagar o erro como `ConnectionError::StreamFailed(...)` usando `?`
  - Critério: zero `expect()` em código de produção fora de testes

- [ ] **Adicionar `#![deny(missing_docs)]` no `lib.rs`**
  - Previne regressão de cobertura de documentação a longo prazo
  - Primeiro adicionar docs faltantes (se houver), depois ativar o deny
  - Critério: `cargo doc --no-deps` não emite warnings de documentação faltante

- [ ] **Adicionar `cargo clippy` como gate de CI** — `.github/workflows/`
  - `cargo clippy --all-features -- -D warnings`
  - Critério: nenhum warning de clippy em PRs

---

## Etapa G — Testes de Robustez e Cancelamento

- [ ] **Testar cancelamento e liberação de recursos do `AcerolaP2p`** — `acerola/tests/`
  - Verificar que dropar `AcerolaP2p` sem chamar `shutdown()` não causa memory leak ou thread órfã
  - Usar `tokio_test` ou `drop()` explícito seguido de sleep para verificar que tasks foram canceladas
  - Critério: nenhum task ativo após drop do `AcerolaP2p`

- [ ] **Testes de property-based para serialização** — `data/identity/`, `infra/peer/`
  - Usar `proptest` para validar roundtrip de `DeviceInfo` e `PeerAddr` com dados arbitrários
  - Verificar que `PeerId::from_public_key` é sempre determinístico para o mesmo input
  - Critério: 1000+ casos gerados automaticamente passam

- [ ] **Adicionar benchmark formal de throughput** — `acerola/benches/`
  - Usar `criterion` para medir throughput do Mock transport vs Iroh
  - Baseline: mock deve processar ≥100 conexões/s; Iroh ≥10 MB/s de throughput
  - Critério: `cargo bench` roda sem erros e gera relatório HTML

---

## Tabela de Verificação Final (Novo)

| Critério | Etapa |
|---|---|
| UniFFI feature compila para Android | A |
| `run()` refatorada em ≤40 linhas | B ✅ |
| Coverage ≥80% no CI | C |
| Módulos `device/`, `error/`, `device_info` com testes | C |
| Score de mutantes ≥75% | D |
| `data/protocol/rpc` não depende de `core::network` | E |
| Zero `expect()` em código de produção | F |
| Clippy clean no CI | F |
| Drop de `AcerolaP2p` sem recursos órfãos | G |
