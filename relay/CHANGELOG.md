# Changelog

Todas as mudanças notáveis deste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/).

## [Unreleased]

### Changed

- **iroh atualizado de `0.98.0` para `1.0.3`** (`iroh-relay`, `iroh-base`, `iroh-dns`). Não foi um bump de número de versão: o código-fonte vendorizado dos três crates foi substituído pelo conteúdo real da tag [`v1.0.3`](https://github.com/n0-computer/iroh/releases/tag/v1.0.3) do upstream (`n0-computer/iroh`), passando pela série de releases `0.98.0 → 0.98.1 → 0.98.2 → 1.0.0-rc.0 → 1.0.0-rc.1 → 1.0.0 → 1.0.1 → 1.0.2 → 1.0.3`.
- `AccessConfig` (enum fechado) foi substituído por `AccessControl` (trait), permitindo rastrear conexões por regra de acesso.
- `CaRootsConfig` foi renomeado para `CaTlsConfig`, agora com suporte a `ServerCertVerifier` customizado.
- Dependência `noq` (motor QUIC) atualizada de `0.18.0` para `1.1.1`.
- `build.rs` do `iroh-relay` não usa mais `vergen`/`vergen-gitcl` para metadados de build via git.

### Added

- Autenticação por Bearer token / auth tokens para acesso ao relay, sem depender de serviço externo de autorização.
- Ajuste de rate limit por cliente em tempo real, sem reiniciar o relay.
- Conexão "happy eyeballs" (tenta IPv4 e IPv6 em paralelo), reduzindo latência de conexão.
- Suporte a múltiplos hostnames com um único certificado Let's Encrypt.
- `Bucket`, a primitiva de rate-limit interna do relay, agora é exposta publicamente.

### Fixed

- Corrigida divisão por zero em `process_datagram`.
- Corrigidas condições de corrida (*race conditions*) que causavam `RemoteStateActor` duplicado e travas no desligamento com requisições pendentes.
- Paths de conexão abandonados deixaram de fechar conexões que não deveriam ser afetadas.
- Endpoint QUIC (`noq`) não é mais encerrado ao primeiro erro transitório de recepção.
- Contador de *fairness* das filas de recepção de transporte corrigido.
- Mensagens malformadas no protocolo do relay agora são tratadas corretamente (antes causavam comportamento indefinido).
- `crossbeam-epoch` atualizado por causa de um *invalid pointer dereference* na dependência (correção de segurança).

### Removed

- Dependências de build ligadas ao `vergen` (`vergen`, `vergen-gitcl`, `vergen-lib`) e ao toolchain de macros `darling`/`derive_builder` que não são mais usadas pelo upstream.

### Dependencies

- 14 crates novos no lockfile (destaque: `axum`, `axum-core`, `tungstenite`, `tokio-tungstenite` — nova stack HTTP/WebSocket do relay). Todos sob licenças permissivas (MIT / Apache-2.0 / BSD-3-Clause), já cobertas por `licenses/README.md`.
- 12 crates removidos (destaque: `vergen*`, `darling*`, `derive_builder*`).
