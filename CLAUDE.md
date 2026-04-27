# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Check (including test code)
cargo check --tests

# Run all tests
cargo test

# Run a single test
cargo test <test_name>

# Format
cargo fmt

# Lint
cargo clippy
```

Tests bind real QUIC sockets via iroh — they are integration-level, not pure unit tests.

## Architecture

The crate is a Rust library (`acerola_p2p`) organized into four internal layers. Each layer may only depend on layers below it. The single upward exception is `From<>` conversions, which the compiler resolves via `?` — not explicit developer dependencies.

```
api  →  core  →  data  →  infra
```

| Layer | Path | Responsibility |
|---|---|---|
| `infra` | `src/infra/` | Raw primitives: `PeerId`, all error enums, `From<>` conversions from external crate types |
| `data` | `src/data/` | Domain entities and contracts: `identity` (seed generation), `protocol` (traits, RPC handler) |
| `core` | `src/core/` | Orchestration: `transport` (QUIC via iroh), `network` (event loop, state), `guard` (connection middleware) |
| `api` | `src/api/` | Public facade — `AcerolaP2p` + `AcerolaP2pBuilder`. Re-exports internal types under clean paths. |

### Key design points

- **`AcerolaP2pBuilder`** collects guards, inbound/outbound ALPN handlers, and a transport builder, then calls `.build()` which spawns `NetworkManager` as a Tokio task and returns `AcerolaP2p`.
- **`NetworkManager`** owns the event loop: accepts connections, runs guard checks, dispatches to registered `ProtocolHandler`s by ALPN, and handles `NetworkCommand`s sent via an mpsc channel.
- **`acerola/rpc`** ALPN is registered automatically on every node (PING/PONG keepalive). Custom protocols are registered via `.inbound()` / `.outbound()`.
- **`IrohTransportBuilder`** accepts an optional `.seed([u8; 32])` for deterministic identity. Without a seed, a random identity is used. `PeerId.device_id` is a UUID v5 derived from the public key via Blake3.
- **`Guard`** (`BoxedValidator`) is an async closure called before accepting any inbound connection. The default allows all peers.

### Module file rule

- Single-concern module → collapse to `mod.rs` (e.g., `infra/peer/mod.rs`).
- Multi-concern module → `mod.rs` declares submodules only; logic lives in child files (e.g., `infra/error/mod.rs` declares `iroh` and `rpc`; the `From<>` impls live in those child files using `use super::` to reference parent enums).

### Public API surface (`src/api/mod.rs` re-exports)

```
acerola_p2p::api::error::P2pError       → infra::error::ConnectionError
acerola_p2p::api::guard::{Guard, ConnectionContext, open_guard}
acerola_p2p::api::peer::PeerIdentity    → infra::peer::PeerId
acerola_p2p::api::protocol::{Handler, EventEmitter}
acerola_p2p::api::network::NetworkMode
acerola_p2p::api::identity::generate_seed
```
