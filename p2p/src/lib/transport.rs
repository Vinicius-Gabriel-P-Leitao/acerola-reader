//! Abstrações das interfaces de transporte base da rede.
//!
//! Contratos (traits) que permitem plugar camadas de transporte diferentes à biblioteca.
//! Todo o acerola-p2p se baseia em instâncias de structs que implementam `P2PTransport` e `IncomingConnection`.

#[path = "transport/iroh.rs"]
pub(crate) mod iroh;

use async_trait::async_trait;
use tokio::io::{AsyncRead, AsyncWrite};

use crate::error::ConnectionError;
use crate::peer::PeerId;

/// Representa um handshake inicial recebido pelo daemon aguardando conversão em fluxos úteis.
#[async_trait]
pub trait IncomingConnection: Send {
    /// O Identificador (Application-Layer Protocol Negotiation) proposto pelo par remoto.
    fn alpn(&self) -> &[u8];

    /// Os dados brutos do vizinho emitente que efetuou a requisição na porta do host.
    fn peer(&self) -> &PeerId;

    /// Promove de fato o socket entrante convertendo-o nas traits `tokio::io` de Leitura/Escrita.
    ///
    /// Esta chamada pode gerar gargalo, portanto é invocada apenas se a camada ALPN
    /// for autorizada e for registrada na biblioteca.
    async fn accept_bi(
        self: Box<Self>,
    ) -> Result<
        (Box<dyn AsyncWrite + Send + Unpin>, Box<dyn AsyncRead + Send + Unpin>),
        ConnectionError,
    >;
}

/// Interface fundamental do Provider físico/lógico responsável por manter os túneis P2P.
///
/// Um implementador de transporte (como o `IrohTransport`) deve instanciar endpoints QUIC/TCP,
/// resolver DNS, prover descoberta Multicast-Mdns e despachar streams de IO.
#[async_trait]
pub trait P2PTransport: Send + Sync {
    /// Fornece as chaves/IDs públicas usadas por este nó.
    fn local_id(&self) -> PeerId;

    /// Loop passivo aguardando requisições de conexão da WAN/LAN.
    ///
    /// Deve entregar um objeto em estado pré-pronto (IncomingConnection)
    /// suspendendo o event loop apenas até ser devidamente acordado.
    async fn accept(&self) -> Result<Box<dyn IncomingConnection>, ConnectionError>;

    /// Inicia uma discagem arbitrária contra outro node, ativamente solicitando um ALPN.
    ///
    /// Em caso de aceite do outro lado, as streams são entregues prontas para comunicação duplex.
    async fn open_bi(
        &self, alpn: &[u8], peer: &PeerId,
    ) -> Result<
        (Box<dyn AsyncWrite + Send + Unpin>, Box<dyn AsyncRead + Send + Unpin>),
        ConnectionError,
    >;

    /// Realiza a destruição graciosa das portas e threads ocupadas pelo driver físico.
    async fn shutdown(&self) -> Result<(), ConnectionError>;
}