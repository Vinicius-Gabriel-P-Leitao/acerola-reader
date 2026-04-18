use std::{collections::HashMap, sync::Arc};
use tokio::sync::{mpsc, RwLock};

use crate::{
    core::connection::p2p::state::network_state::NetworkState,
    infra::{
        error::messages::connection_error::ConnectionError,
        remote::p2p::{
            guard::BoxedValidator, peer_id::PeerId, protocol_handler::ProtocolHandler,
            transport::P2PTransport,
        },
    },
};

pub enum NetworkCommand {
    SwitchGuard { validator: BoxedValidator },
    Connect { peer: PeerId, alpn: Vec<u8> },
    Shutdown,
}

pub struct NetworkManager {
    transport: Arc<dyn P2PTransport>,
    state: Arc<RwLock<NetworkState>>,
    validator: Arc<RwLock<BoxedValidator>>,
    command_tx: mpsc::UnboundedSender<NetworkCommand>,
    command_rx: mpsc::UnboundedReceiver<NetworkCommand>,
    handlers: HashMap<Vec<u8>, Arc<dyn ProtocolHandler>>,
}

impl NetworkManager {
    pub fn new(
        transport: Arc<dyn P2PTransport>, validator: BoxedValidator,
    ) -> (Self, mpsc::UnboundedSender<NetworkCommand>, Arc<RwLock<NetworkState>>) {
        let (command_tx, command_rx) = mpsc::unbounded_channel();
        let state = Arc::new(RwLock::new(NetworkState::new()));

        let manager = Self {
            transport,
            command_rx,
            handlers: HashMap::new(),
            command_tx: command_tx.clone(),
            state: Arc::clone(&state),
            validator: Arc::new(RwLock::new(validator)),
        };

        (manager, command_tx, state)
    }

    pub fn register(&mut self, alpn: &[u8], handler: Arc<dyn ProtocolHandler>) {
        self.handlers.insert(alpn.to_vec(), handler);
    }

    pub fn state(&self) -> Arc<RwLock<NetworkState>> {
        Arc::clone(&self.state)
    }

    pub async fn run(mut self) {
        loop {
            tokio::select! {
                result = self.transport.accept() => {
                    match result {
                        Ok((alpn, peer, send, recv)) => {
                            let Some(handler) = self.handlers.get(&alpn) else { continue };
                            let state = Arc::clone(&self.state);
                            let handler = handler.clone();

                            state.write().await.connect(peer.clone(), alpn.clone());

                            tokio::spawn(async move {
                                let _ = handler.handle(&peer, send, recv).await;
                                state.write().await.disconnect(&peer);
                            });
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
                        NetworkCommand::SwitchGuard { validator } => {
                            *self.validator.write().await = validator;
                        }
                        NetworkCommand::Shutdown => break,
                    }
                }
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::{
        infra::{error::messages::connection_error, remote::p2p::guard::BoxedValidator},
        tests::utils::mock_transport::mock_transport,
    };
    use tokio::time::{sleep, Duration};

    fn open_validator() -> BoxedValidator {
        Box::new(|_ctx| Box::pin(async { Ok(()) }))
    }

    fn make_peer(id: &str) -> PeerId {
        PeerId { id: id.to_string() }
    }

    #[tokio::test]
    async fn handler_registrado_para_alpn_e_encontrado() {
        let (transport, _handle) = mock_transport();
        let (mut manager, _, _) = NetworkManager::new(Arc::new(transport), open_validator());

        struct NoopHandler;
        #[async_trait::async_trait]
        impl ProtocolHandler for NoopHandler {
            async fn handle(
                &self, _peer: &PeerId, _send: Box<dyn tokio::io::AsyncWrite + Send + Unpin>,
                _recv: Box<dyn tokio::io::AsyncRead + Send + Unpin>,
            ) -> Result<(), connection_error::ConnectionError> {
                Ok(())
            }
        }

        manager.register(b"acerola/rpc", Arc::new(NoopHandler));
        assert!(manager.handlers.contains_key(b"acerola/rpc".as_ref()));
    }

    #[tokio::test]
    async fn peer_adicionado_ao_state_ao_aceitar_conexao() {
        let (transport, handle) = mock_transport();
        let transport: Arc<dyn P2PTransport> = Arc::new(transport);
        let (mut manager, _, state) = NetworkManager::new(Arc::clone(&transport), open_validator());

        struct NoopHandler;
        #[async_trait::async_trait]
        impl ProtocolHandler for NoopHandler {
            async fn handle(
                &self, _peer: &PeerId, _send: Box<dyn tokio::io::AsyncWrite + Send + Unpin>,
                _recv: Box<dyn tokio::io::AsyncRead + Send + Unpin>,
            ) -> Result<(), connection_error::ConnectionError> {
                sleep(Duration::from_millis(50)).await;
                Ok(())
            }
        }

        manager.register(b"acerola/rpc", Arc::new(NoopHandler));

        let (client, server) = tokio::io::duplex(1024);
        handle.inject(b"acerola/rpc", make_peer("peer-1"), client, server);

        tokio::spawn(manager.run());
        sleep(Duration::from_millis(20)).await;

        assert!(state.read().await.is_connected(&make_peer("peer-1")));
    }

    #[tokio::test]
    async fn peer_removido_do_state_quando_handler_termina() {
        let (transport, handle) = mock_transport();
        let transport: Arc<dyn P2PTransport> = Arc::new(transport);
        let (mut manager, _, state) = NetworkManager::new(Arc::clone(&transport), open_validator());

        struct NoopHandler;
        #[async_trait::async_trait]
        impl ProtocolHandler for NoopHandler {
            async fn handle(
                &self, _peer: &PeerId, _send: Box<dyn tokio::io::AsyncWrite + Send + Unpin>,
                _recv: Box<dyn tokio::io::AsyncRead + Send + Unpin>,
            ) -> Result<(), connection_error::ConnectionError> {
                Ok(())
            }
        }

        manager.register(b"acerola/rpc", Arc::new(NoopHandler));

        let (client, server) = tokio::io::duplex(1024);
        handle.inject(b"acerola/rpc", make_peer("peer-2"), client, server);

        tokio::spawn(manager.run());
        sleep(Duration::from_millis(50)).await;

        assert!(!state.read().await.is_connected(&make_peer("peer-2")));
    }

    #[tokio::test]
    async fn alpn_desconhecido_e_ignorado() {
        let (transport, handle) = mock_transport();
        let transport: Arc<dyn P2PTransport> = Arc::new(transport);
        let (manager, _, state) = NetworkManager::new(Arc::clone(&transport), open_validator());

        let (client, server) = tokio::io::duplex(1024);
        handle.inject(b"acerola/unknown", make_peer("peer-3"), client, server);

        tokio::spawn(manager.run());
        sleep(Duration::from_millis(20)).await;

        assert!(!state.read().await.is_connected(&make_peer("peer-3")));
    }

    #[tokio::test]
    async fn shutdown_encerra_o_loop() {
        let (transport, _handle) = mock_transport();
        let (manager, command_tx, _) = NetworkManager::new(Arc::new(transport), open_validator());

        let handle = tokio::spawn(manager.run());
        let _ = command_tx.send(NetworkCommand::Shutdown);

        let result = tokio::time::timeout(Duration::from_millis(100), handle).await;
        assert!(result.is_ok());
    }
}
