use std::{collections::HashMap, sync::Arc};
use tokio::sync::mpsc;

use crate::infra::{
    error::messages::connection_error::ConnectionError,
    remote::p2p::{peer_id::PeerId, protocol_handler::ProtocolHandler, transport::P2PTransport},
};

pub enum NetworkCommand {
    Connect { peer: PeerId, alpn: Vec<u8> },
    Shutdown,
}

pub struct NetworkManager {
    transport: Arc<dyn P2PTransport>,
    handlers: HashMap<Vec<u8>, Arc<dyn ProtocolHandler>>,
    command_rx: mpsc::UnboundedReceiver<NetworkCommand>,
    command_tx: mpsc::UnboundedSender<NetworkCommand>,
}

impl NetworkManager {
    fn new(transport: Arc<dyn P2PTransport>) -> (Self, mpsc::UnboundedSender<NetworkCommand>) {
        let (command_tx, command_rx) = mpsc::unbounded_channel();

        let manager = Self {
            transport,
            handlers: HashMap::new(),l
            command_rx,
            command_tx: command_tx.clone(),
        };

        (manager, command_tx)
    }

    fn register(&mut self, alpn: &[u8], handler: Arc<dyn ProtocolHandler>) {
        self.handlers.insert(alpn.to_vec(), handler);
    }

    async fn run(mut self) {
        loop {
            tokio::select! {
                result = self.transport.accept() => {
                    match result {
                        Ok((alpn, peer, send, recv)) => {
                            if let Some(handler) = self.handlers.get(&alpn) {
                                let handler = handler.clone();
                                tokio::spawn(async move {
                                    let _ = handler.handle(&peer, send, recv).await;
                                });
                            }
                        }
                        Err(ConnectionError::Shutdown) => break,
                        Err(_) => continue,
                    }
                }
                Some(cmd) = self.command_rx.recv() => {
                    match cmd {
                        NetworkCommand::Connect { peer, alpn } => {
                            let _ = self.transport.open_bi(&alpn, &peer).await;
                        }
                        NetworkCommand::Shutdown => break,
                    }
                }
            }
        }
    }
}
