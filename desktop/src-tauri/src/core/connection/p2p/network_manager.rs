use std::{collections::HashMap, sync::Arc};
use tokio::sync::{mpsc, RwLock};

use crate::{
    core::connection::p2p::state::network_state::{NetworkMode, NetworkState},
    infra::{
        error::messages::connection_error::ConnectionError,
        remote::p2p::{
            guard::BoxedValidator, peer_id::PeerId, protocol_handler::ProtocolHandler,
            transport::P2PTransport,
        },
    },
};

pub enum NetworkCommand {
    SwitchGuard { validator: BoxedValidator, mode: NetworkMode },
    Connect { peer: PeerId, alpn: Vec<u8> },
    Shutdown,
}

pub struct NetworkManager {
    transport: Arc<dyn P2PTransport>,
    state: Arc<RwLock<NetworkState>>,
    validator: Arc<RwLock<BoxedValidator>>,
    command_tx: mpsc::UnboundedSender<NetworkCommand>,
    command_rx: mpsc::UnboundedReceiver<NetworkCommand>,
    handlers_inbound: HashMap<Vec<u8>, Arc<dyn ProtocolHandler>>,
    handlers_outbound: HashMap<Vec<u8>, Arc<dyn ProtocolHandler>>,
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
            command_tx: command_tx.clone(),
            state: Arc::clone(&state),
            handlers_inbound: HashMap::new(),
            handlers_outbound: HashMap::new(),
            validator: Arc::new(RwLock::new(validator)),
        };

        (manager, command_tx, state)
    }

    pub fn state(&self) -> Arc<RwLock<NetworkState>> {
        Arc::clone(&self.state)
    }

    pub fn register_inbound(&mut self, alpn: &[u8], handler: Arc<dyn ProtocolHandler>) {
        self.handlers_inbound.insert(alpn.to_vec(), handler);
    }

    pub fn register_outbound(&mut self, alpn: &[u8], handler: Arc<dyn ProtocolHandler>) {
        self.handlers_outbound.insert(alpn.to_vec(), handler);
    }

    pub async fn run(mut self) {
        loop {
            tokio::select! {
                result = self.transport.accept() => {
                    match result {
                        Ok(incoming) => {
                            let Some(handler) = self.handlers_inbound.get(incoming.alpn()) else { continue };
                            let state = Arc::clone(&self.state);
                            let handler = handler.clone();

                            tokio::spawn(async move {
                                let peer = incoming.peer().clone();
                                let alpn = incoming.alpn().to_vec();
                                let Ok((send, recv)) = incoming.accept_bi().await else { return };

                                state.write().await.connect(peer.clone(), alpn);
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
                            let Some(handler) = self.handlers_outbound.get(&alpn) else { continue };

                            let state = Arc::clone(&self.state);
                            let handler = handler.clone();

                            let peer_clone = peer.clone();
                            let alpn_clone = alpn.clone();

                            match self.transport.open_bi(&alpn, &peer).await {
                                Ok((send, recv)) => {
                                    state.write().await.connect(peer_clone.clone(), alpn_clone);
                                    tokio::spawn(async move {
                                        let _ = handler.handle(&peer_clone, send, recv).await;
                                        state.write().await.disconnect(&peer_clone);
                                    });
                                }
                                Err(_) => {}
                            }
                        }
                        NetworkCommand::SwitchGuard { validator, mode } => {
                            *self.validator.write().await = validator;
                            self.state.write().await.switch_mode(mode);
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

    struct SlowHandler;
    #[async_trait::async_trait]
    impl ProtocolHandler for SlowHandler {
        async fn handle(
            &self, _peer: &PeerId, _send: Box<dyn tokio::io::AsyncWrite + Send + Unpin>,
            _recv: Box<dyn tokio::io::AsyncRead + Send + Unpin>,
        ) -> Result<(), connection_error::ConnectionError> {
            sleep(Duration::from_millis(50)).await;
            Ok(())
        }
    }

    #[tokio::test]
    async fn handler_inbound_registrado_para_alpn_e_encontrado() {
        let (transport, _handle) = mock_transport();
        let (mut manager, _, _) = NetworkManager::new(Arc::new(transport), open_validator());

        manager.register_inbound(b"acerola/rpc", Arc::new(NoopHandler));

        assert!(manager.handlers_inbound.contains_key(b"acerola/rpc".as_ref()));
    }

    #[tokio::test]
    async fn handler_outbound_registrado_para_alpn_e_encontrado() {
        let (transport, _handle) = mock_transport();
        let (mut manager, _, _) = NetworkManager::new(Arc::new(transport), open_validator());

        manager.register_outbound(b"acerola/rpc", Arc::new(NoopHandler));

        assert!(manager.handlers_outbound.contains_key(b"acerola/rpc".as_ref()));
    }

    #[tokio::test]
    async fn peer_adicionado_ao_state_ao_aceitar_conexao() {
        let (transport, handle) = mock_transport();
        let transport: Arc<dyn P2PTransport> = Arc::new(transport);
        let (mut manager, _, state) = NetworkManager::new(Arc::clone(&transport), open_validator());

        manager.register_inbound(b"acerola/rpc", Arc::new(SlowHandler));

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

        manager.register_inbound(b"acerola/rpc", Arc::new(NoopHandler));

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
