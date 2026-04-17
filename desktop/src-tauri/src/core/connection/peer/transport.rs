use async_trait::async_trait;
use tokio::io::{AsyncRead, AsyncWrite};

use crate::core::connection::peer::types::PeerId;
use crate::infra::error::translations::connection_error::ConnectionError;

#[async_trait]
pub trait P2PTransport: Send + Sync {
    /// Retorna a identidade local na rede.
    /// Síncrono porque é só uma leitura de valor já inicializado.
    fn local_id(&self) -> PeerId;

    /// Abre um canal bidirecional com um peer em um protocolo específico.
    /// alpn identifica o protocolo: b"acerola/gql", b"acerola/rpc", etc.
    /// Retorna dois lados independentes: um para escrever, um para ler.
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
    >;

    /// Encerra o endpoint limpo.
    async fn shutdown(&self) -> Result<(), ConnectionError>;
}
