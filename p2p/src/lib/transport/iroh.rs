//! Implementa��o baseada no Iroh como motor Quic/UDP principal.
//!
//! Este m�dulo converte o sistema do endpoint Iroh e conectividade hole-punching
//! para os traits gen�ricos `P2PTransport` e `IncomingConnection` do ecossistema local.

use async_trait::async_trait;
use iroh::address_lookup::mdns;
use iroh::endpoint::presets;
use iroh::{Endpoint, EndpointAddr, EndpointId};
use iroh::{RelayConfig, RelayMap, RelayUrl};
use std::pin::Pin;
use std::sync::Arc;
use std::task::{Context, Poll};
use tokio::io::{AsyncRead, AsyncWrite, ReadBuf};

use crate::error::ConnectionError;
use crate::peer::PeerId;
use crate::transport::{IncomingConnection, P2pTransport, TransportP2pBuilder};

/// Embalagem da estrutura de conex�o transit�ria `iroh::endpoint::Connection`.
pub struct IrohIncoming {
    conn: iroh::endpoint::Connection,
    peer: PeerId,
    alpn: Vec<u8>,
}

/// Wrapper que mant�m a inst�ncia do `iroh::endpoint::Connection` viva enquanto a stream de escrita for utilizada.
pub struct ConnectionWriter {
    inner: iroh::endpoint::SendStream,
    _conn: Arc<iroh::endpoint::Connection>,
}

impl AsyncWrite for ConnectionWriter {
    fn poll_write(
        mut self: Pin<&mut Self>, cx: &mut Context<'_>, buf: &[u8],
    ) -> Poll<Result<usize, std::io::Error>> {
        Pin::new(&mut self.inner)
            .poll_write(cx, buf)
            .map_err(|err| std::io::Error::new(std::io::ErrorKind::Other, err))
    }

    fn poll_flush(
        mut self: Pin<&mut Self>, cx: &mut Context<'_>,
    ) -> Poll<Result<(), std::io::Error>> {
        Pin::new(&mut self.inner)
            .poll_flush(cx)
            .map_err(|err: std::io::Error| std::io::Error::new(std::io::ErrorKind::Other, err))
    }

    fn poll_shutdown(
        mut self: Pin<&mut Self>, cx: &mut Context<'_>,
    ) -> Poll<Result<(), std::io::Error>> {
        Pin::new(&mut self.inner)
            .poll_shutdown(cx)
            .map_err(|err| std::io::Error::new(std::io::ErrorKind::Other, err))
    }
}

/// Wrapper que mant�m a inst�ncia do `iroh::endpoint::Connection` viva enquanto a stream de leitura for utilizada.
pub struct ConnectionReader {
    inner: iroh::endpoint::RecvStream,
    _conn: Arc<iroh::endpoint::Connection>,
}

impl AsyncRead for ConnectionReader {
    fn poll_read(
        mut self: Pin<&mut Self>, cx: &mut Context<'_>, buf: &mut ReadBuf<'_>,
    ) -> Poll<Result<(), std::io::Error>> {
        Pin::new(&mut self.inner)
            .poll_read(cx, buf)
            .map_err(|err| std::io::Error::new(std::io::ErrorKind::Other, err))
    }
}

/// Interface concreta que gerencia o Endpoint UDP local e a configura��o de chaves usando a suite Iroh.
pub struct IrohTransport {
    /// Refer�ncia mantida viva para os contextos globais e loops do iroh-net subjacente.
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
        let shared_conn = Arc::new(self.conn);
        Ok((
            Box::new(ConnectionWriter { inner: send, _conn: Arc::clone(&shared_conn) }),
            Box::new(ConnectionReader { inner: recv, _conn: shared_conn }),
        ))
    }
}

impl IrohTransport {
    /// Trata a convers�o sint�tica das Strings em NodeIds estritos nativos do iroh.
    #[rustfmt::skip]
    fn peer_to_addr(&self, peer: &PeerId) -> Result<EndpointAddr, ConnectionError> {
        let id: EndpointId = peer.id.parse().map_err(|_| ConnectionError::PeerNotFound(PeerId { id: peer.id.clone() }))?;
        Ok(EndpointAddr::from(id))
    }
}

#[async_trait]
impl P2pTransport for IrohTransport {
    fn local_id(&self) -> PeerId {
        // Converte o EndpointID para o meu PeerId
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

        let shared_conn = Arc::new(conn);
        Ok((
            Box::new(ConnectionWriter { inner: send, _conn: Arc::clone(&shared_conn) }),
            Box::new(ConnectionReader { inner: recv, _conn: shared_conn }),
        ))
    }

    /// Executa o teardown for�ado do componente iroh.
    ///
    /// Warn: O endpoint � compartilhado em formato Arc no backend do crate `iroh`.
    /// Desligar essa faceta pode necessitar dropar todos os componentes de leitura remanescentes.
    async fn shutdown(&self) -> Result<(), ConnectionError> {
        self.endpoint.close().await;
        Ok(())
    }
}

pub struct IrohTransportBuilder {
    relay_urls: Vec<String>,
}

impl Default for IrohTransportBuilder {
    fn default() -> Self {
        Self { relay_urls: Vec::new() }
    }
}

impl IrohTransportBuilder {
    pub fn relay(mut self, url: &str) -> Self {
        self.relay_urls.push(url.to_string());
        self
    }
}

#[async_trait]
impl TransportP2pBuilder for IrohTransportBuilder {
    type Output = IrohTransport;

    /// Inicia um novo endpoint na rede usando as portas dispon�veis do host.
    ///
    /// mantendo o mDNS local como fallback ao inv�s de fixar o preset N0.
    #[rustfmt::skip]
    async fn build(self, alpns: Vec<Vec<u8>>) -> Result<IrohTransport, ConnectionError> {
        let mdns = mdns::MdnsAddressLookup::builder();

        let relay_configs: Vec<RelayConfig> = self.relay_urls.into_iter()
            .map(|url| {
                url.parse::<RelayUrl>().map(|relay_url| RelayConfig { url: relay_url, quic: None })
            }).collect::<Result<_, iroh::RelayUrlParseError>>()?;

        // Faz o bind inicializando o discovery via rede local e os recursos P2P ALPN autorizados por padrão, o preset é para usar os DNS do N0 para resolver o relay.
        let endpoint = Endpoint::builder(presets::N0)
            .relay_mode(iroh::RelayMode::Custom(
                RelayMap::from_iter(relay_configs)
            )).alpns(alpns).address_lookup(mdns).bind().await?;

        Ok(IrohTransport { endpoint })
    }
}
