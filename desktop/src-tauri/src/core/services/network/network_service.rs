use crate::{
    core::connection::p2p::{network_manager::NetworkCommand, state::network_state::NetworkState},
    infra::remote::p2p::{peer_id::PeerId, transport::P2PTransport},
};

use std::{collections::HashMap, sync::Arc};
use tokio::sync::{mpsc, RwLock};

pub struct NetworkService {
    state: Arc<RwLock<NetworkState>>,
    transport: Arc<dyn P2PTransport>,
    command_tx: mpsc::UnboundedSender<NetworkCommand>,
}

impl NetworkService {
    #[rustfmt::skip]
    pub fn new(
        state: Arc<RwLock<NetworkState>>, 
        transport: Arc<dyn P2PTransport>,
        command_tx: mpsc::UnboundedSender<NetworkCommand>,
    ) -> Self {
        Self { transport, command_tx, state }
    }

    pub fn local_id(&self) -> PeerId {
        self.transport.local_id()
    }

    pub async fn connected_peers(&self) -> HashMap<PeerId, Vec<u8>> {
        self.state.read().await.peers().clone()
    }

    pub fn connect(&self, peer: PeerId, alpn: Vec<u8>) {
        let _ = self.command_tx.send(NetworkCommand::Connect { peer, alpn });
    }

    pub fn shutdown(&self) {
        let _ = self.command_tx.send(NetworkCommand::Shutdown);
    }
}
