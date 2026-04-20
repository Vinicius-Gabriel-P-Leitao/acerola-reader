use async_trait::async_trait;
use tokio::io::{AsyncRead, AsyncWrite};

use crate::infra::error::messages::connection_error::ConnectionError;
use crate::infra::remote::p2p::peer_id::PeerId;

#[async_trait]
pub trait IncomingConnection: Send {
    fn alpn(&self) -> &[u8];
    fn peer(&self) -> &PeerId;

    async fn accept_bi(
        self: Box<Self>,
    ) -> Result<
        (Box<dyn AsyncWrite + Send + Unpin>, Box<dyn AsyncRead + Send + Unpin>),
        ConnectionError,
    >;
}

#[async_trait]
pub trait P2PTransport: Send + Sync {
    fn local_id(&self) -> PeerId;

    async fn accept(&self) -> Result<Box<dyn IncomingConnection>, ConnectionError>;

    async fn open_bi(
        &self, alpn: &[u8], peer: &PeerId,
    ) -> Result<
        (Box<dyn AsyncWrite + Send + Unpin>, Box<dyn AsyncRead + Send + Unpin>),
        ConnectionError,
    >;

    async fn shutdown(&self) -> Result<(), ConnectionError>;
}
