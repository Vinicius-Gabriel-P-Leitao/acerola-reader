//! Crate `acerola_p2p` — biblioteca P2P modular para comunicação peer-to-peer.

#![deny(missing_docs)]

pub(crate) mod core;
pub(crate) mod data;
pub(crate) mod infra;

#[cfg(test)]
pub mod tests;

pub mod api;
