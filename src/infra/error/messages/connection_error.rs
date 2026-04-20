use thiserror::Error;

use crate::infra::remote::p2p::peer_id::PeerId;

#[derive(Debug, Error)]
pub enum ConnectionError {
    #[error("peer not found: {0}")]
    PeerNotFound(PeerId),

    #[error("connection denied by guard")]
    AuthDenied,

    #[error("stream failed: {0}")]
    StreamFailed(String),

    #[error("endpoint shut down")]
    Shutdown,

    #[error("connection timed out")]
    Timeout,

    #[error("peer disconnected")]
    PeerDisconnected,

    #[error("incompatible protocol version")]
    IncompatibleVersion,

    #[error("failed to initialize connection: {0}")]
    StartupFailed(String),
}
