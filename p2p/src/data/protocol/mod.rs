pub(crate) mod rpc;

use std::sync::Arc;

use async_trait::async_trait;
use tokio::io::{AsyncRead, AsyncWrite};

use crate::{
    data::identity::device_info::DeviceInfo,
    infra::{error::ConnectionError, peer::PeerId},
};

/// Type alias para callback emissor de eventos do sistema.
pub type EventEmitter = Arc<dyn Fn(&str, String) + Send + Sync>;

/// Abstração para armazenamento de informações de dispositivos remotos.
#[async_trait]
pub trait DeviceInfoStore: Send + Sync {
    /// Armazena as informações de um dispositivo associado a um peer remoto.
    async fn store_device_info(&self, peer: PeerId, info: DeviceInfo);
}

/// Contrato para manipuladores de protocolo de aplicação sobre streams bidirecionais.
#[async_trait]
pub trait ProtocolHandler: Send + Sync {
    /// Manipula o fluxo de dados bidirecional de um protocolo para determinado peer.
    async fn handle(
        &self, peer: &PeerId, send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), ConnectionError>;
}
