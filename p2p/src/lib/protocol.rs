//! Abstrações para implementação de protocolos e dispatchers na biblioteca.
//!
//! Qualquer comunicação no `acerola-p2p` baseia-se em multiplexação de streams
//! sobre ALPN (Application-Layer Protocol Negotiation). Este módulo fornece as
//! definições base para a criação de rotinas de comunicação padronizadas.

#[path = "protocol/rpc.rs"]
pub(crate) mod rpc;

use std::sync::Arc;

use async_trait::async_trait;
use tokio::io::{AsyncRead, AsyncWrite};

use crate::error::ConnectionError;
use crate::peer::PeerId;

/// Tipo de função delegada para emitir eventos para a aplicação cliente.
///
/// Permite que protocolos despachem notificações assíncronas (ex: `"rpc:ping_received"`)
/// informando alterações de estado ou o recebimento de mensagens.
pub type EventEmitter = Arc<dyn Fn(&str, String) + Send + Sync>;

/// Interface obrigatória para manipulação do ciclo de vida de uma conexão ALPN.
///
/// Essa trait deve ser implementada para cada tipo de serviço da rede (RPC, sync, etc.).
/// O `NetworkManager` delega o processamento da stream subjacente para as instâncias
/// que implementam esta trait assim que o handshake da conexão é validado.
#[async_trait]
pub trait ProtocolHandler: Send + Sync {
    /// Inicia o loop de processamento da conexão P2P.
    ///
    /// # Parâmetros
    /// * `peer` - Informações sobre o nó remoto conectado.
    /// * `send` - Stream assíncrona voltada apenas para escrita.
    /// * `recv` - Stream assíncrona voltada apenas para leitura.
    ///
    /// A execução dessa função deve ser contínua enquanto a conexão estiver viva.
    /// Quando a função retorna, as streams subjacentes são derrubadas.
    async fn handle(
        &self, peer: &PeerId, send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), ConnectionError>;
}