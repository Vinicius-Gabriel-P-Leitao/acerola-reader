use async_trait::async_trait;
use iroh::{endpoint::presets, Endpoint, EndpointAddr, EndpointId};
use tokio::io::{AsyncRead, AsyncWrite};

use crate::infra::error::messages::connection_error::ConnectionError;
use crate::infra::remote::p2p::{transport::P2PTransport, peer_id::PeerId};

pub struct IrohTransport {
    endpoint: Endpoint,
}

impl IrohTransport {
    /// FIXME: Atualmente usa o preset N0 — será substituído pelo relay próprio.
    pub async fn new() -> Result<Self, ConnectionError> {
        let endpoint = Endpoint::builder(presets::N0).bind().await?;
        Ok(Self { endpoint })
    }

    fn peer_to_addr(&self, peer: &PeerId) -> Result<EndpointAddr, ConnectionError> {
        let id: EndpointId = peer.id.parse().map_err(|_| {
            ConnectionError::PeerNotFound(PeerId {
                id: peer.id.clone(),
            })
        })?;

        Ok(EndpointAddr::from(id))
    }
}

#[async_trait]
impl P2PTransport for IrohTransport {
    fn local_id(&self) -> PeerId {
        PeerId {
            id: self.endpoint.id().to_string(),
        }
    }

    async fn open_bi(
        &self,
        alpn: &[u8],
        peer: &PeerId,
    ) -> Result<
        (
            Box<dyn AsyncWrite + Send + Unpin>,
            Box<dyn AsyncRead + Send + Unpin>,
        ),
        ConnectionError,
    > {
        let addr = self.peer_to_addr(peer)?;
        let conn = self.endpoint.connect(addr, alpn).await?;
        let (send, recv) = conn.open_bi().await?;

        Ok((Box::new(send), Box::new(recv)))
    }

    /// WARN: Os sockets UDP só fecham quando todos os clones do Endpoint são dropados.
    async fn shutdown(&self) -> Result<(), ConnectionError> {
        self.endpoint.close().await;
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_new_cria_endpoint() {
        let transport = IrohTransport::new().await;
        assert!(transport.is_ok());
    }

    #[tokio::test]
    async fn test_local_id_nao_vazio() {
        let transport = IrohTransport::new().await.unwrap();
        let id = transport.local_id();
        assert!(!id.id.is_empty());
    }

    #[tokio::test]
    async fn test_shutdown_limpo() {
        let transport = IrohTransport::new().await.unwrap();
        let result = transport.shutdown().await;
        assert!(result.is_ok());
    }
}
