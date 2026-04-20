pub mod api;
pub use api::{
    AcerolaP2P, AcerolaP2PBuilder, EventEmitter, Guard, Handler, P2PError, PeerIdentity,
};

#[path = "lib/error.rs"]
pub(crate) mod error;

#[path = "lib/guard.rs"]
pub(crate) mod guard;

#[path = "lib/network.rs"]
pub(crate) mod network;

#[path = "lib/peer.rs"]
pub(crate) mod peer;

#[path = "lib/protocol.rs"]
pub(crate) mod protocol;

#[path = "lib/transport.rs"]
pub(crate) mod transport;

#[cfg(test)]
pub(crate) mod tests;
