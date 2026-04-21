use std::{collections::HashMap, sync::Arc};

pub mod error {
    pub use crate::error::ConnectionError as P2PError;
}
pub mod guard {
    pub use crate::guard::{open_guard, BoxedValidator as Guard, ConnectionContext};
}
pub mod peer {
    pub use crate::peer::PeerId as PeerIdentity;
}
pub mod protocol {
    pub use crate::protocol::{EventEmitter, ProtocolHandler as Handler};
}
pub mod network {
    pub use crate::network::state::NetworkMode;
}

use crate::{
    api::network::NetworkMode,
    error::ConnectionError,
    guard::BoxedValidator,
    network::{NetworkCommand, NetworkManager},
    peer::PeerId,
    protocol::{
        rpc::{RpcClientHandler, RpcServerHandler},
        EventEmitter, ProtocolHandler,
    },
    transport::{iroh::IrohTransport, P2PTransport},
};

pub struct AcerolaP2PBuilder {
    emit: EventEmitter,
    guard: BoxedValidator,
    handlers_inbound: HashMap<Vec<u8>, Arc<dyn ProtocolHandler>>,
    handlers_outbound: HashMap<Vec<u8>, Arc<dyn ProtocolHandler>>,
}

impl AcerolaP2PBuilder {
    fn new(emit: EventEmitter) -> Self {
        Self {
            emit,
            guard: Box::new(|_ctx| Box::pin(async { Ok(()) })),
            handlers_inbound: HashMap::new(),
            handlers_outbound: HashMap::new(),
        }
    }

    pub fn guard(mut self, validator: BoxedValidator) -> Self {
        self.guard = validator;
        self
    }

    pub fn inbound(mut self, alpn: &[u8], handler: Arc<dyn ProtocolHandler>) -> Self {
        self.handlers_inbound.insert(alpn.to_vec(), handler);
        self
    }

    pub fn outbound(mut self, alpn: &[u8], handler: Arc<dyn ProtocolHandler>) -> Self {
        self.handlers_outbound.insert(alpn.to_vec(), handler);
        self
    }

    pub async fn build(self) -> Result<AcerolaP2P, ConnectionError> {
        let transport = Arc::new(IrohTransport::new().await?);
        let local_id = transport.local_id();

        let (mut manager, command_tx, _state) =
            NetworkManager::new(Arc::clone(&transport) as Arc<dyn P2PTransport>, self.guard);

        manager.register_inbound(
            b"acerola/rpc",
            Arc::new(RpcServerHandler::new(Arc::clone(&self.emit))),
        );
        manager.register_outbound(
            b"acerola/rpc",
            Arc::new(RpcClientHandler::new(Arc::clone(&self.emit))),
        );

        for (alpn, handler) in self.handlers_inbound {
            manager.register_inbound(&alpn, handler);
        }
        for (alpn, handler) in self.handlers_outbound {
            manager.register_outbound(&alpn, handler);
        }

        tokio::spawn(manager.run());

        Ok(AcerolaP2P { command_tx, local_id })
    }
}

pub struct AcerolaP2P {
    command_tx: tokio::sync::mpsc::Sender<NetworkCommand>,
    local_id: PeerId,
}

impl AcerolaP2P {
    pub fn builder(emit: EventEmitter) -> AcerolaP2PBuilder {
        AcerolaP2PBuilder::new(emit)
    }

    pub fn local_id(&self) -> &str {
        &self.local_id.id
    }

    pub async fn connect(&self, peer_id: &str, alpn: &[u8]) -> Result<(), ConnectionError> {
        let peer = PeerId { id: peer_id.to_string() };
        self.command_tx
            .send(NetworkCommand::Connect { peer, alpn: alpn.to_vec() })
            .await
            .map_err(|_| ConnectionError::Shutdown)
    }

    pub async fn switch_guard(
        &self, validator: crate::guard::BoxedValidator, mode: NetworkMode,
    ) -> Result<(), ConnectionError> {
        self.command_tx
            .send(NetworkCommand::SwitchGuard { validator, mode })
            .await
            .map_err(|_| ConnectionError::Shutdown)
    }

    pub async fn shutdown(&self) -> Result<(), ConnectionError> {
        self.command_tx.send(NetworkCommand::Shutdown).await.map_err(|_| ConnectionError::Shutdown)
    }
}
