//! Abstrações das interfaces de transporte base da rede.
//!
//! Contratos (traits) que permitem plugar camadas de transporte diferentes à biblioteca.
//! Todo o acerola-p2p se baseia em instâncias de structs que implementam `P2PTransport` e `IncomingConnection`.

/// Implementação concreta do transporte baseada no protocolo QUIC/Iroh.
#[cfg(feature = "iroh")]
pub mod iroh;

use async_trait::async_trait;
use tokio::io::{AsyncRead, AsyncWrite};

use crate::infra::{
    error::ConnectionError,
    peer::{PeerAddr, PeerId},
};

/// Representa um handshake inicial recebido pelo daemon aguardando conversão em fluxos úteis.
#[async_trait]
pub trait IncomingConnection: Send {
    /// O Identificador (Application-Layer Protocol Negotiation) proposto pelo par remoto.
    fn alpn(&self) -> &[u8];

    /// Os dados brutos do vizinho emitente que efetuou a requisição na porta do host.
    fn peer(&self) -> &PeerId;

    /// O endereço completo para discagem reversa se necessário.
    fn addr(&self) -> &PeerAddr;

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
pub trait P2pTransport: Send + Sync {
    /// Fornece as chaves/IDs públicas usadas por este nó.
    fn local_id(&self) -> PeerId;

    /// Fornece o endereço para discagem, retorna o PeerId dentro também
    fn local_addr(&self) -> Result<PeerAddr, ConnectionError>;

    /// Loop passivo aguardando requisições de conexão da WAN/LAN.
    ///
    /// Deve entregar um objeto em estado pré-pronto (IncomingConnection)
    /// suspendendo o event loop apenas até ser devidamente acordado.
    async fn accept(&self) -> Result<Box<dyn IncomingConnection>, ConnectionError>;

    /// Inicia uma discagem arbitrária contra outro node, ativamente solicitando um ALPN.
    ///
    /// Em caso de aceite do outro lado, as streams são entregues prontas para comunicação duplex.
    async fn open_bi(
        &self, alpn: &[u8], peer: &PeerAddr,
    ) -> Result<
        (Box<dyn AsyncWrite + Send + Unpin>, Box<dyn AsyncRead + Send + Unpin>),
        ConnectionError,
    >;

    /// Retorna a estimativa de latência (RTT) para um par específico, se houver conexão ativa.
    async fn latency(&self, peer: &PeerId) -> Option<std::time::Duration>;

    /// Realiza a destruição graciosa das portas e threads ocupadas pelo driver físico.
    async fn shutdown(&self) -> Result<(), ConnectionError>;
}

/// Interface para montar um transporte responsável por montar o transporte antes de qualquer coisa
#[async_trait]
pub trait TransportP2pBuilder: Send + Sync {
    /// O tipo de transporte produzido por este builder.
    type Output: P2pTransport;

    /// Constrói a instância de transporte com os ALPNs configurados.
    async fn build(self, alpns: Vec<Vec<u8>>) -> Result<Self::Output, ConnectionError>;

    /// Atribui explicitamente a seed para derivação da chave secreta.
    fn set_seed(&mut self, _seed: [u8; 32]) {}

    /// Retorna a seed configurada, caso exista.
    fn get_seed(&self) -> Option<[u8; 32]> {
        None
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    struct MinimalDummyBuilder;

    #[async_trait]
    impl TransportP2pBuilder for MinimalDummyBuilder {
        type Output = crate::tests::mock_transport::MockTransport;
        async fn build(self, _alpns: Vec<Vec<u8>>) -> Result<Self::Output, ConnectionError> {
            let (transport, _handle) = crate::tests::mock_transport::mock_transport();
            Ok(transport)
        }
    }

    #[test]
    fn default_trait_methods_behave_as_expected() {
        let mut dummy_builder = MinimalDummyBuilder;
        dummy_builder.set_seed([0xAA; 32]);
        assert!(dummy_builder.get_seed().is_none());
    }
}
