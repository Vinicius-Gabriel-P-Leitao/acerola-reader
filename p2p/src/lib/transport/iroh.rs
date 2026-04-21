//! Implementação baseada no Iroh como motor Quic/UDP principal.
//!
//! Este módulo converte o sistema do endpoint Iroh e conectividade hole-punching
//! para os traits genéricos `P2PTransport` e `IncomingConnection` do ecossistema local.

use async_trait::async_trait;
use iroh::address_lookup::mdns;
use iroh::{endpoint::presets, Endpoint, EndpointAddr, EndpointId};
use tokio::io::{AsyncRead, AsyncWrite};

use crate::error::ConnectionError;
use crate::peer::PeerId;
use crate::transport::{IncomingConnection, P2PTransport};

/// Embalagem da estrutura de conexão transitória `iroh::endpoint::Connection`.
pub struct IrohIncoming {
    conn: iroh::endpoint::Connection,
    peer: PeerId,
    alpn: Vec<u8>,
}

/// Interface concreta que gerencia o Endpoint UDP local e a configuração de chaves usando a suite Iroh.
pub struct IrohTransport {
    /// Referência mantida viva para os contextos globais e loops do iroh-net subjacente.
    endpoint: Endpoint,
}

#[async_trait]
impl IncomingConnection for IrohIncoming {
    fn alpn(&self) -> &[u8] {
        &self.alpn
    }

    fn peer(&self) -> &PeerId {
        &self.peer
    }

    async fn accept_bi(
        self: Box<Self>,
    ) -> Result<
        (Box<dyn AsyncWrite + Send + Unpin>, Box<dyn AsyncRead + Send + Unpin>),
        ConnectionError,
    > {
        let (send, recv) = self.conn.accept_bi().await?;
        Ok((Box::new(send), Box::new(recv)))
    }
}

impl IrohTransport {
    /// Inicia um novo endpoint na rede usando as portas disponíveis do host.
    ///
    // TODO: Permitir que o código que usa a lib passe o endpoint do relay configurado,
    // mantendo o mDNS local como fallback ao invés de fixar o preset N0.
    pub async fn new() -> Result<Self, ConnectionError> {
        let mdns = mdns::MdnsAddressLookup::builder();

        // Faz o bind inicializando o discovery via rede local e os recursos P2P ALPN autorizados por padrão.
        let endpoint = Endpoint::builder(presets::N0)
            .alpns(vec![b"acerola/rpc".to_vec()])
            .address_lookup(mdns)
            .bind()
            .await?;

        Ok(Self { endpoint })
    }

    /// Trata a conversão sintática das Strings em NodeIds estritos nativos do iroh.
    fn peer_to_addr(&self, peer: &PeerId) -> Result<EndpointAddr, ConnectionError> {
        let id: EndpointId = peer
            .id
            .parse()
            .map_err(|_| ConnectionError::PeerNotFound(PeerId { id: peer.id.clone() }))?;

        Ok(EndpointAddr::from(id))
    }
}

#[async_trait]
impl P2PTransport for IrohTransport {
    fn local_id(&self) -> PeerId {
        PeerId { id: self.endpoint.id().to_string() }
    }

    async fn accept(&self) -> Result<Box<dyn IncomingConnection>, ConnectionError> {
        let incoming = self.endpoint.accept().await.ok_or(ConnectionError::Shutdown)?;
        let conn = incoming.await?;

        Ok(Box::new(IrohIncoming {
            peer: PeerId { id: conn.remote_id().to_string() },
            alpn: conn.alpn().to_vec(),
            conn,
        }))
    }

    async fn open_bi(
        &self, alpn: &[u8], peer: &PeerId,
    ) -> Result<
        (Box<dyn AsyncWrite + Send + Unpin>, Box<dyn AsyncRead + Send + Unpin>),
        ConnectionError,
    > {
        let addr = self.peer_to_addr(peer)?;
        let conn = self.endpoint.connect(addr, alpn).await?;
        let (send, recv) = conn.open_bi().await?;

        Ok((Box::new(send), Box::new(recv)))
    }

    /// Executa o teardown forçado do componente iroh.
    ///
    /// Warn: O endpoint é compartilhado em formato Arc no backend do crate `iroh`.
    /// Desligar essa faceta pode necessitar dropar todos os componentes de leitura remanescentes.
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