use async_trait::async_trait;
use tokio::io::{AsyncRead, AsyncWrite};

use crate::infra::{
    error::messages::connection_error::ConnectionError, remote::p2p::peer_id::PeerId,
};

#[async_trait]
pub trait ProtocolHandler: Send + Sync {
    async fn handle(
        &self, peer: &PeerId, send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), ConnectionError>;
}
