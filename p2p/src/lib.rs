//! Crate `acerola_p2p` — biblioteca P2P modular para comunicação peer-to-peer.

#![deny(missing_docs)]

/// Módulo central de infraestrutura e gerenciamento de rede P2P.
pub mod core;
pub(crate) mod data;
pub(crate) mod infra;

/// Módulo de testes unitários e de integração.
pub mod tests;

/// Fachada pública e Builder do nó Acerola.
pub mod api;
