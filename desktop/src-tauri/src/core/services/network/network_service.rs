use crate::{
    core::connection::p2p::{
        network_manager::NetworkCommand,
        state::network_state::{NetworkMode, NetworkState},
    },
    data::remote::p2p::{open_guard::open_guard, token_guard::token_guard},
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

    pub fn switch_to_local(&self) {
        let _ = self.command_tx.send(NetworkCommand::SwitchGuard {
            validator: Box::new(|ctx| Box::pin(open_guard(ctx))),
            mode: NetworkMode::Local,
        });
    }

    pub fn switch_to_relay(&self) {
        let _ = self.command_tx.send(NetworkCommand::SwitchGuard {
            validator: Box::new(|ctx| Box::pin(token_guard(ctx))),
            mode: NetworkMode::Relay,
        });
    }

    pub async fn mode(&self) -> NetworkMode {
        let state = self.state.read().await;
        state.mode().clone()
    }

    pub fn connect(&self, peer: PeerId, alpn: Vec<u8>) {
        let _ = self.command_tx.send(NetworkCommand::Connect { peer, alpn });
    }

    pub fn shutdown(&self) {
        let _ = self.command_tx.send(NetworkCommand::Shutdown);
    }
}
