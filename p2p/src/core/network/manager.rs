//! Gestão de ciclos de execução, roteamento e controle central da rede.
//!
//! O `NetworkManager` atua como o cérebro assíncrono da biblioteca.
//! Ele encapsula a instância de transporte físico (ex: Iroh), executa o laço de eventos
//! (event loop) para aceitar conexões ativamente, e faz a ponte (dispatch) entre os
//! canais I/O recém-chegados e o respectivo `ProtocolHandler` mapeado para o ALPN requisitado.

use std::{collections::HashMap, sync::Arc};

use futures::SinkExt;
use tokio::{
    io::AsyncWriteExt,
    sync::{mpsc, RwLock},
    task::JoinSet,
};
use tokio_util::codec::{FramedWrite, LengthDelimitedCodec};
use tracing::Instrument;

use crate::{
    core::{
        guard::{BoxedValidator, ConnectionContext},
        network::state::{NetworkMode, NetworkState},
        storage::P2PStorage,
        transport::P2pTransport,
    },
    data::protocol::{EventEmitter, ProtocolHandler},
    infra::{
        error::ConnectionError,
        peer::{PeerAddr, PeerId},
    },
};

/// Limite de comandos simultâneos não processados na fila do loop principal.
const COMMAND_CHANNEL_CAPACITY: usize = 64;

/// Byte constante enviado no handshake de encerramento da conexão.
const GOODBYE: u8 = 0x03;

/// Sinais de controle enviados ao Event Loop da rede.
///
/// Como o manager é blindado e opera numa tarefa em background, toda interação
/// externa é sinalizada e enfileirada no canal por meio deste enum.
pub enum NetworkCommand {
    /// Troca dinâmica da política de validação (Guard) e estado nominal da rede.
    SwitchGuard {
        /// O novo validador Guard a ser aplicado.
        validator: BoxedValidator,
        /// O novo modo de operação da rede.
        mode: NetworkMode,
    },
    /// Tenta discar ativamente para outro par através de um protocolo.
    Connect {
        /// Endereço de destino do peer.
        addr: PeerAddr,
        /// Protocolo ALPN requisitado.
        alpn: Vec<u8>,
    },
    /// Provoca a desmontagem e desligamento seguro do daemon P2P.
    Shutdown,
}

/// Motor principal do nó acerola-p2p, responsável pelo event loop e orquestração.
pub struct NetworkManager {
    /// Provedor base responsável por I/O e alocação de sockets (Iroh).
    transport: Arc<dyn P2pTransport>,
    /// Referência concorrente para o estado (peers conectados, etc).
    state: Arc<RwLock<NetworkState>>,
    /// Referência do Guard atual ativo para validação no aceite de conexões.
    validator: Arc<RwLock<BoxedValidator>>,
    /// Componente de persistência de identidade e peers (P2PStorage).
    storage: Option<Arc<dyn P2PStorage>>,
    /// Fila para consumo dos comandos requisitados externamente.
    command_rx: mpsc::Receiver<NetworkCommand>,
    /// Tabela de protocolos autorizados para quem recebe conexões (Servidor).
    handlers_inbound: HashMap<Vec<u8>, Arc<dyn ProtocolHandler>>,
    /// Tabela de protocolos operados por quem inicia conexões (Cliente).
    handlers_outbound: HashMap<Vec<u8>, Arc<dyn ProtocolHandler>>,
    /// Canal de emissão de eventos assíncronos para a camada de aplicação/UI.
    emit: EventEmitter,
}

impl NetworkManager {
    /// Inicializa os componentes internos de gerência de rede.
    ///
    /// Cria e compartilha buffers MPSC e o `NetworkState`. Retorna uma tupla
    /// contendo a instância pronta para rodar, o comunicador (sender) e a view
    /// do estado da rede, garantindo que o chamador mantenha as referências ativas.
    #[allow(dead_code)]
    pub fn new(
        transport: Arc<dyn P2pTransport>, validator: BoxedValidator, emit: EventEmitter,
    ) -> (Self, mpsc::Sender<NetworkCommand>, Arc<RwLock<NetworkState>>) {
        Self::with_storage(transport, validator, emit, None)
    }

    /// Inicializa os componentes internos de gerência de rede com suporte opcional a storage.
    pub fn with_storage(
        transport: Arc<dyn P2pTransport>, validator: BoxedValidator, emit: EventEmitter,
        storage: Option<Arc<dyn P2PStorage>>,
    ) -> (Self, mpsc::Sender<NetworkCommand>, Arc<RwLock<NetworkState>>) {
        let (command_tx, command_rx) = mpsc::channel(COMMAND_CHANNEL_CAPACITY);
        let state = Arc::new(RwLock::new(NetworkState::new()));

        let manager = Self {
            transport,
            command_rx,
            state: Arc::clone(&state),
            handlers_inbound: HashMap::new(),
            handlers_outbound: HashMap::new(),
            validator: Arc::new(RwLock::new(validator)),
            storage,
            emit,
        };

        (manager, command_tx, state)
    }

    /// Registra um serviço voltado ao recebimento passivo de conexões.
    ///
    /// Mapeia uma string ALPN a um `ProtocolHandler`. Ao receber conexões com este ALPN,
    /// a conexão passará primeiro pelo guard e, se permitida, será roteada a este tratador.
    pub fn register_inbound(&mut self, alpn: &[u8], handler: Arc<dyn ProtocolHandler>) {
        self.handlers_inbound.insert(alpn.to_vec(), handler);
    }

    /// Registra um serviço focado no disparo ativo de conexões ao ecossistema.
    ///
    /// Mapeia o ALPN em cenários em que o nó é proativo (dialer) em invocar funcionalidades.
    pub fn register_outbound(&mut self, alpn: &[u8], handler: Arc<dyn ProtocolHandler>) {
        self.handlers_outbound.insert(alpn.to_vec(), handler);
    }

    /// Assume o controle da Thread e dispara o laço principal de IO da biblioteca.
    ///
    /// Deve ser convocado via `tokio::spawn(manager.run())`. Durante este loop infinito
    /// ele espera assincronamente (multiplexando pelo `tokio::select!`) por conexões
    /// externas advindas do Transporte e comandos oriundos dos canais MPSC locais.
    pub async fn run(mut self) {
        let mut latency_interval = tokio::time::interval(std::time::Duration::from_secs(30));
        latency_interval.set_missed_tick_behavior(tokio::time::MissedTickBehavior::Skip);

        loop {
            tokio::select! {
                _ = latency_interval.tick() => {
                    self.emit_latency_for_all_peers().await;
                }
                result = self.transport.accept() => {
                    match result {
                        Ok(incoming) => self.handle_incoming(incoming),
                        Err(ConnectionError::Shutdown) => break,
                        Err(err) => {
                            tracing::debug!(error = ?err, "transport accept failed");
                        }
                    }
                }
                cmd_option = self.command_rx.recv() => {
                    match cmd_option {
                        Some(NetworkCommand::Connect { addr, alpn }) => {
                            self.handle_connect_command(addr, alpn);
                        }
                        Some(NetworkCommand::SwitchGuard { validator, mode }) => {
                            self.handle_switch_guard(validator, mode).await;
                        }
                        Some(NetworkCommand::Shutdown) => {
                            self.broadcast_goodbye().await;
                            break;
                        }
                        None => {
                            // O canal command_tx foi fechado (ex: a instância AcerolaP2p foi dropada sem shutdown explícito)
                            break;
                        }
                    }
                }
            }
        }
    }

    /// Emite eventos de latência para todos os peers atualmente conectados.
    async fn emit_latency_for_all_peers(&self) {
        let peers: Vec<PeerId> = self.state.read().await.peers().keys().cloned().collect();

        for peer in peers {
            if let Some(latency) = self.transport.latency(&peer).await {
                let payload = serde_json::json!({
                    "peer_id": peer.id,
                    "latency_ms": latency.as_millis() as u64,
                })
                .to_string();
                (self.emit)("network:latency", payload);
            }
        }
    }

    /// Aceita e despacha uma conexão inbound em background.
    ///
    /// Ignora silenciosamente conexões cujo ALPN não possui handler registrado.
    fn handle_incoming(&self, incoming: Box<dyn crate::core::transport::IncomingConnection>) {
        let Some(handler) = self.handlers_inbound.get(incoming.alpn()) else { return };

        let state = Arc::clone(&self.state);
        let handler = handler.clone();
        let validator = Arc::clone(&self.validator);
        let storage = self.storage.clone();

        let span = tracing::info_span!(
            "inbound",
            peer = %incoming.peer().id,
            alpn = ?String::from_utf8_lossy(incoming.alpn())
        );

        tokio::spawn(
            async move {
                let peer = incoming.peer().clone();
                let addr = incoming.addr().clone();
                let alpn = incoming.alpn().to_vec();

                let Ok((send, recv)) = incoming.accept_bi().await else { return };

                let ctx = ConnectionContext { peer_id: peer.clone(), data: () };
                let allowed = {
                    let guard = validator.read().await;
                    guard(&ctx)
                }
                .await;

                if let Err(err) = allowed {
                    tracing::debug!(error = ?err, "connection denied by guard");
                    return;
                }

                if save_peer_if_present(storage.as_ref(), &addr).await.is_err() {
                    tracing::error!(peer = %peer.id, "failed to save peer to storage, terminating connection");
                    return;
                }

                state.write().await.connect(peer.clone(), addr, alpn.clone());
                tracing::debug!("connection accepted");

                if let Err(err) = handler.handle(&peer, send, recv).await {
                    tracing::warn!(error = ?err, "inbound handler failed");
                }
                tracing::debug!("connection closed");

                state.write().await.disconnect(&peer, &alpn);
            }
            .instrument(span),
        );
    }

    /// Dispara uma tentativa de conexão outbound com retries em background.
    ///
    /// Ignora silenciosamente se não há handler registrado para o ALPN solicitado.
    fn handle_connect_command(&self, addr: PeerAddr, alpn: Vec<u8>) {
        let Some(handler) = self.handlers_outbound.get(&alpn) else { return };

        let state = Arc::clone(&self.state);
        let handler = handler.clone();
        let transport = Arc::clone(&self.transport);
        let storage = self.storage.clone();

        let span = tracing::info_span!(
            "outbound",
            addr = %addr.id,
            alpn = ?String::from_utf8_lossy(&alpn)
        );

        tokio::spawn(
            async move {
                let max_retries = 5;
                let mut backoff = std::time::Duration::from_millis(100);

                for attempt in 1..=max_retries {
                    match transport.open_bi(&alpn, &addr).await {
                        Ok((send, recv)) => {
                            if save_peer_if_present(storage.as_ref(), &addr).await.is_err() {
                                tracing::error!(peer = %addr.id, "failed to save outbound peer to storage, terminating connection");
                                return;
                            }

                            state.write().await.connect(addr.id.clone(), addr.clone(), alpn.clone());
                            tracing::debug!("outbound connection established");

                            if let Err(err) = handler.handle(&addr.id, send, recv).await {
                                tracing::warn!(error = ?err, "outbound handler failed");
                            }

                            tracing::debug!("outbound connection closed");
                            state.write().await.disconnect(&addr.id, &alpn);
                            return;
                        }
                        Err(err) => {
                            tracing::warn!(
                                attempt,
                                error = ?err,
                                "outbound connection attempt failed, retrying..."
                            );
                            if attempt < max_retries {
                                tokio::time::sleep(backoff).await;
                                backoff = (backoff * 2).min(std::time::Duration::from_secs(5));
                            }
                        }
                    }
                }
            }
            .instrument(span),
        );
    }

    /// Substitui on-the-fly o Guard ativo e o modo operacional da rede.
    async fn handle_switch_guard(&self, validator: BoxedValidator, mode: NetworkMode) {
        *self.validator.write().await = validator;
        self.state.write().await.switch_mode(mode);
    }

    /// Envia o sinal de GOODBYE para todos os peers conectados antes de desligar.
    async fn broadcast_goodbye(&self) {
        let addrs: Vec<PeerAddr> = {
            let state = self.state.read().await;
            state.peers().keys().filter_map(|peer_id| state.get_addr(peer_id).cloned()).collect()
        };

        if addrs.is_empty() {
            return;
        }

        tracing::info!(count = addrs.len(), "broadcasting goodbye to all peers");

        let mut set = JoinSet::new();

        for addr in addrs {
            let transport = Arc::clone(&self.transport);

            set.spawn(async move {
                if let Err(err) = Self::send_goodbye_to_peer(transport, addr.clone()).await {
                    tracing::warn!(peer = %addr.id, ?err, "failed to send goodbye");
                }
            });
        }

        // Aguarda todas as tarefas de sinalização concluírem antes de prosseguir com o shutdown físico.
        while set.join_next().await.is_some() {}
    }

    /// Lógica atômica para abrir uma stream temporária e sinalizar a saída.
    async fn send_goodbye_to_peer(
        transport: Arc<dyn P2pTransport>, addr: PeerAddr,
    ) -> Result<(), ConnectionError> {
        let (send, _recv) = transport.open_bi(b"acerola/handshake/1", &addr).await?;
        let mut writer = FramedWrite::new(send, LengthDelimitedCodec::new());

        writer.send(vec![GOODBYE].into()).await?;
        writer.flush().await?;
        writer.get_mut().shutdown().await?;

        tracing::debug!(peer = %addr.id, "goodbye sent gracefully");
        Ok(())
    }
}

/// Utilitário funcional para persistir peer apenas se o storage estiver configurado.
async fn save_peer_if_present(
    storage: Option<&Arc<dyn P2PStorage>>, addr: &PeerAddr,
) -> Result<(), ConnectionError> {
    match storage {
        Some(storage) => storage.save_peer(addr).await,
        None => Ok(()),
    }
}

#[cfg(test)]
mod tests {
    use std::sync::Mutex;

    use tokio::{
        io::AsyncReadExt,
        time::{sleep, Duration},
    };

    use super::*;
    use crate::{infra::peer::PeerId, tests::mock_transport::mock_transport};

    fn open_validator() -> BoxedValidator {
        Box::new(|_ctx| Box::pin(async { Ok(()) }))
    }

    fn no_op_emitter() -> EventEmitter {
        Arc::new(|_event: &str, _payload: String| {})
    }

    #[allow(clippy::type_complexity)]
    fn capture_emitter() -> (EventEmitter, Arc<Mutex<Vec<(String, String)>>>) {
        let events = Arc::new(Mutex::new(Vec::new()));
        let clone = Arc::clone(&events);
        let emit: EventEmitter = Arc::new(move |event: &str, payload: String| {
            clone.lock().unwrap().push((event.to_string(), payload));
        });
        (emit, events)
    }

    fn make_peer(id: &str) -> PeerId {
        PeerId { id: id.to_string(), device_id: None }
    }

    struct NoopHandler;
    #[async_trait::async_trait]
    impl ProtocolHandler for NoopHandler {
        async fn handle(
            &self, _peer: &PeerId, _send: Box<dyn tokio::io::AsyncWrite + Send + Unpin>,
            _recv: Box<dyn tokio::io::AsyncRead + Send + Unpin>,
        ) -> Result<(), ConnectionError> {
            Ok(())
        }
    }

    struct SlowHandler;
    #[async_trait::async_trait]
    impl ProtocolHandler for SlowHandler {
        async fn handle(
            &self, _peer: &PeerId, _send: Box<dyn tokio::io::AsyncWrite + Send + Unpin>,
            _recv: Box<dyn tokio::io::AsyncRead + Send + Unpin>,
        ) -> Result<(), ConnectionError> {
            sleep(Duration::from_millis(50)).await;
            Ok(())
        }
    }

    #[tokio::test]
    async fn inbound_handler_registered_for_alpn_is_found() {
        let (transport, _handle) = mock_transport();
        let (mut manager, _, _) =
            NetworkManager::new(Arc::new(transport), open_validator(), no_op_emitter());
        manager.register_inbound(b"acerola/handshake/1", Arc::new(NoopHandler));
        assert!(manager.handlers_inbound.contains_key(b"acerola/handshake/1".as_ref()));
    }

    #[tokio::test]
    async fn outbound_handler_registered_for_alpn_is_found() {
        let (transport, _handle) = mock_transport();
        let (mut manager, _, _) =
            NetworkManager::new(Arc::new(transport), open_validator(), no_op_emitter());
        manager.register_outbound(b"acerola/handshake/1", Arc::new(NoopHandler));
        assert!(manager.handlers_outbound.contains_key(b"acerola/handshake/1".as_ref()));
    }

    #[tokio::test]
    async fn peer_added_to_state_on_accepting_connection() {
        let (transport, handle) = mock_transport();
        let transport: Arc<dyn P2pTransport> = Arc::new(transport);
        let (mut manager, _command_tx, state) =
            NetworkManager::new(Arc::clone(&transport), open_validator(), no_op_emitter());
        manager.register_inbound(b"acerola/handshake/1", Arc::new(SlowHandler));

        let (client, server) = tokio::io::duplex(1024);
        handle.inject(b"acerola/handshake/1", make_peer("peer-1"), client, server);

        tokio::spawn(manager.run());
        sleep(Duration::from_millis(20)).await;

        assert!(state.read().await.is_connected(&make_peer("peer-1")));
    }

    #[tokio::test]
    async fn peer_removed_from_state_when_handler_finishes() {
        let (transport, handle) = mock_transport();
        let transport: Arc<dyn P2pTransport> = Arc::new(transport);
        let (mut manager, _command_tx, state) =
            NetworkManager::new(Arc::clone(&transport), open_validator(), no_op_emitter());
        manager.register_inbound(b"acerola/handshake/1", Arc::new(NoopHandler));

        let (client, server) = tokio::io::duplex(1024);
        handle.inject(b"acerola/handshake/1", make_peer("peer-2"), client, server);

        tokio::spawn(manager.run());
        sleep(Duration::from_millis(50)).await;

        assert!(!state.read().await.is_connected(&make_peer("peer-2")));
    }

    #[tokio::test]
    async fn unknown_alpn_is_ignored() {
        let (transport, handle) = mock_transport();
        let transport: Arc<dyn P2pTransport> = Arc::new(transport);
        let (manager, _command_tx, state) =
            NetworkManager::new(Arc::clone(&transport), open_validator(), no_op_emitter());

        let (client, server) = tokio::io::duplex(1024);
        handle.inject(b"acerola/unknown", make_peer("peer-3"), client, server);

        tokio::spawn(manager.run());
        sleep(Duration::from_millis(20)).await;

        assert!(!state.read().await.is_connected(&make_peer("peer-3")));
    }

    #[tokio::test]
    async fn shutdown_terminates_loop() {
        let (transport, _handle) = mock_transport();
        let (manager, command_tx, _) =
            NetworkManager::new(Arc::new(transport), open_validator(), no_op_emitter());

        let handle = tokio::spawn(manager.run());
        let _ = command_tx.send(NetworkCommand::Shutdown).await;

        let result = tokio::time::timeout(Duration::from_millis(100), handle).await;
        assert!(result.is_ok());
    }

    #[tokio::test]
    async fn guard_denies_connection_from_blocked_peer() {
        let (transport, handle) = mock_transport();
        let transport: Arc<dyn P2pTransport> = Arc::new(transport);

        let deny_all: BoxedValidator = Box::new(|_ctx| {
            Box::pin(async { Err(ConnectionError::AuthDenied("test deny all".into())) })
        });

        let (mut manager, _command_tx, state) =
            NetworkManager::new(Arc::clone(&transport), deny_all, no_op_emitter());
        manager.register_inbound(b"acerola/handshake/1", Arc::new(SlowHandler));

        let (client, server) = tokio::io::duplex(1024);
        handle.inject(b"acerola/handshake/1", make_peer("peer-blocked"), client, server);

        tokio::spawn(manager.run());
        sleep(Duration::from_millis(30)).await;

        assert!(!state.read().await.is_connected(&make_peer("peer-blocked")));
    }

    #[tokio::test]
    async fn same_peer_on_two_alpns_appears_connected() {
        let (transport, handle) = mock_transport();
        let transport: Arc<dyn P2pTransport> = Arc::new(transport);
        let (mut manager, _command_tx, state) =
            NetworkManager::new(Arc::clone(&transport), open_validator(), no_op_emitter());

        manager.register_inbound(b"acerola/handshake/1", Arc::new(SlowHandler));
        manager.register_inbound(b"acerola/blob/1", Arc::new(SlowHandler));

        let (c1, s1) = tokio::io::duplex(1024);
        let (c2, s2) = tokio::io::duplex(1024);
        handle.inject(b"acerola/handshake/1", make_peer("peer-multi"), c1, s1);
        handle.inject(b"acerola/blob/1", make_peer("peer-multi"), c2, s2);

        tokio::spawn(manager.run());
        sleep(Duration::from_millis(20)).await;

        assert!(state.read().await.is_connected(&make_peer("peer-multi")));
        assert!(state
            .read()
            .await
            .is_connected_on(&make_peer("peer-multi"), b"acerola/handshake/1"));
        assert!(state.read().await.is_connected_on(&make_peer("peer-multi"), b"acerola/blob/1"));
    }

    #[tokio::test]
    async fn latency_event_emitted_for_connected_peers() {
        let (transport, handle) = mock_transport();
        let peer = make_peer("peer-latency");
        handle.set_latency(peer.clone(), Duration::from_millis(42)).await;

        let (emit, events) = capture_emitter();
        let transport: Arc<dyn P2pTransport> = Arc::new(transport);
        let (mut manager, _command_tx, _state) =
            NetworkManager::new(Arc::clone(&transport), open_validator(), emit);
        manager.register_inbound(b"acerola/handshake/1", Arc::new(SlowHandler));

        let (client, server) = tokio::io::duplex(1024);
        handle.inject(b"acerola/handshake/1", peer.clone(), client, server);

        tokio::spawn(manager.run());
        sleep(Duration::from_millis(50)).await;

        let captured = events.lock().unwrap();
        assert!(captured.iter().any(|(ev, payload)| {
            ev == "network:latency" && payload.contains("peer-latency") && payload.contains("42")
        }));
    }

    struct FailingStorage;
    #[async_trait::async_trait]
    impl P2PStorage for FailingStorage {
        async fn save_identity(&self, _secret: &[u8]) -> Result<(), ConnectionError> {
            Ok(())
        }
        async fn load_identity(&self) -> Result<Option<Vec<u8>>, ConnectionError> {
            Ok(None)
        }
        async fn save_peer(&self, _peer: &PeerAddr) -> Result<(), ConnectionError> {
            Err(ConnectionError::StreamFailed("disk write error".to_string()))
        }
        async fn load_peers(&self) -> Result<Vec<PeerAddr>, ConnectionError> {
            Ok(vec![])
        }
    }

    #[tokio::test]
    async fn inbound_connection_terminated_and_no_ghost_peer_when_save_peer_fails() {
        let (transport, handle) = mock_transport();
        let transport: Arc<dyn P2pTransport> = Arc::new(transport);
        let storage = Arc::new(FailingStorage);

        let (mut manager, _command_tx, state) = NetworkManager::with_storage(
            Arc::clone(&transport),
            open_validator(),
            no_op_emitter(),
            Some(storage),
        );
        manager.register_inbound(b"acerola/handshake/1", Arc::new(SlowHandler));

        let (client, server) = tokio::io::duplex(1024);
        handle.inject(b"acerola/handshake/1", make_peer("peer-storage-fail"), client, server);

        tokio::spawn(manager.run());
        sleep(Duration::from_millis(30)).await;

        assert!(!state.read().await.is_connected(&make_peer("peer-storage-fail")));
    }

    #[tokio::test]
    async fn shutdown_with_connected_peers_broadcasts_goodbye() {
        // Testa se o shutdown com peers conectados dispara a transmissão de goodbye sem travar o loop
        let (transport, transport_handle) = mock_transport();
        let transport: Arc<dyn P2pTransport> = Arc::new(transport);

        let (mut network_manager, command_sender, network_state) =
            NetworkManager::new(Arc::clone(&transport), open_validator(), no_op_emitter());

        network_manager.register_inbound(b"acerola/handshake/1", Arc::new(SlowHandler));

        let peer_alpha = make_peer("peer-alpha");
        let peer_beta = make_peer("peer-beta");

        let (client_alpha, server_alpha) = tokio::io::duplex(1024);
        let (client_beta, server_beta) = tokio::io::duplex(1024);

        transport_handle.inject(
            b"acerola/handshake/1",
            peer_alpha.clone(),
            client_alpha,
            server_alpha,
        );
        transport_handle.inject(
            b"acerola/handshake/1",
            peer_beta.clone(),
            client_beta,
            server_beta,
        );

        let manager_task_handle = tokio::spawn(network_manager.run());
        sleep(Duration::from_millis(30)).await;

        // Garante que os dois pares foram registrados no estado nominal da rede
        assert!(network_state.read().await.is_connected(&peer_alpha));
        assert!(network_state.read().await.is_connected(&peer_beta));

        // Pré-aloca os buffers para as duas transmissões de goodbye no open_bi
        let (outbound_client_first, mut outbound_server_first) = tokio::io::duplex(1024);
        let (outbound_client_second, mut outbound_server_second) = tokio::io::duplex(1024);
        let (_dummy_client_first, dummy_server_first) = tokio::io::duplex(1024);
        let (_dummy_client_second, dummy_server_second) = tokio::io::duplex(1024);

        transport_handle.expect_open(outbound_client_first, dummy_server_first);
        transport_handle.expect_open(outbound_client_second, dummy_server_second);

        // Dispara a sinalização de Shutdown
        let send_result = command_sender.send(NetworkCommand::Shutdown).await;
        assert!(send_result.is_ok());

        // Confirma encerramento do event loop dentro do tempo limite
        let timeout_result =
            tokio::time::timeout(Duration::from_millis(500), manager_task_handle).await;
        assert!(timeout_result.is_ok());

        // Valida que a mensagem de GOODBYE foi efetivamente transmitida pelos canais outbound
        let mut buffer_alpha = [0u8; 16];
        let mut buffer_beta = [0u8; 16];
        let read_alpha_result = tokio::time::timeout(
            Duration::from_millis(100),
            outbound_server_first.read(&mut buffer_alpha),
        )
        .await;
        let read_beta_result = tokio::time::timeout(
            Duration::from_millis(100),
            outbound_server_second.read(&mut buffer_beta),
        )
        .await;

        assert!(
            read_alpha_result.is_ok() && read_alpha_result.unwrap().unwrap_or(0) > 0,
            "Should transmit goodbye payload to peer-alpha"
        );
        assert!(
            read_beta_result.is_ok() && read_beta_result.unwrap().unwrap_or(0) > 0,
            "Should transmit goodbye payload to peer-beta"
        );
    }

    #[tokio::test]
    async fn switch_guard_command_updates_active_validator_and_network_mode() {
        let (transport, _transport_handle) = mock_transport();
        let (network_manager, command_sender, network_state) =
            NetworkManager::new(Arc::new(transport), open_validator(), no_op_emitter());

        let manager_task_handle = tokio::spawn(network_manager.run());

        let deny_validator: BoxedValidator = Box::new(|_ctx| {
            Box::pin(async { Err(ConnectionError::AuthDenied("switch test deny".into())) })
        });

        command_sender
            .send(NetworkCommand::SwitchGuard {
                validator: deny_validator,
                mode: crate::core::network::state::NetworkMode::Relay,
            })
            .await
            .unwrap();

        sleep(Duration::from_millis(30)).await;

        let state_guard = network_state.read().await;
        assert_eq!(*state_guard.mode(), crate::core::network::state::NetworkMode::Relay);

        let _shutdown_result = command_sender.send(NetworkCommand::Shutdown).await;
        let _manager_join_result = manager_task_handle.await;
    }

    struct TrackingBackoffTransport {
        call_timestamps: Arc<tokio::sync::Mutex<Vec<tokio::time::Instant>>>,
    }

    #[async_trait::async_trait]
    impl P2pTransport for TrackingBackoffTransport {
        fn local_id(&self) -> PeerId {
            PeerId { id: "test-peer".to_string(), device_id: None }
        }

        fn local_addr(&self) -> Result<PeerAddr, ConnectionError> {
            Ok(PeerAddr { id: self.local_id(), addrs: vec![] })
        }

        async fn accept(
            &self,
        ) -> Result<Box<dyn crate::core::transport::IncomingConnection>, ConnectionError> {
            std::future::pending().await
        }

        async fn open_bi(
            &self, _alpn: &[u8], _peer: &PeerAddr,
        ) -> Result<
            (
                Box<dyn tokio::io::AsyncWrite + Send + Unpin>,
                Box<dyn tokio::io::AsyncRead + Send + Unpin>,
            ),
            ConnectionError,
        > {
            self.call_timestamps.lock().await.push(tokio::time::Instant::now());
            Err(ConnectionError::StreamFailed("simulated network failure".to_string()))
        }

        async fn latency(&self, _peer: &PeerId) -> Option<Duration> {
            None
        }

        async fn shutdown(&self) -> Result<(), ConnectionError> {
            Ok(())
        }
    }

    #[tokio::test(start_paused = true)]
    async fn exponential_backoff_increases_delay_and_terminates_safely() {
        // Inicializa o vetor de marcas temporais para registrar o momento exato de cada tentativa
        let call_timestamps = Arc::new(tokio::sync::Mutex::new(Vec::new()));
        let tracking_transport =
            Arc::new(TrackingBackoffTransport { call_timestamps: Arc::clone(&call_timestamps) });

        let (mut network_manager, command_sender, _network_state) =
            NetworkManager::new(tracking_transport, open_validator(), no_op_emitter());

        network_manager.register_outbound(b"acerola/test/1", Arc::new(NoopHandler));

        let manager_task_handle = tokio::spawn(network_manager.run());

        let target_peer_address = PeerAddr { id: make_peer("target-peer"), addrs: vec![] };

        command_sender
            .send(NetworkCommand::Connect {
                addr: target_peer_address,
                alpn: b"acerola/test/1".to_vec(),
            })
            .await
            .unwrap();

        // Aguarda virtualmente tempo suficiente para que todas as 5 tentativas concluam
        tokio::time::sleep(Duration::from_millis(3000)).await;

        let timestamps_guard = call_timestamps.lock().await;

        // Confirma que exatamente 5 tentativas foram executadas e que a task terminou sem pânico
        assert_eq!(timestamps_guard.len(), 5, "Exactly 5 reconnection attempts should occur");

        // Calcula os intervalos entre tentativas consecutivas
        let first_interval = timestamps_guard[1] - timestamps_guard[0];
        let second_interval = timestamps_guard[2] - timestamps_guard[1];
        let third_interval = timestamps_guard[3] - timestamps_guard[2];
        let fourth_interval = timestamps_guard[4] - timestamps_guard[3];

        // Confirma que os intervalos seguem a progressão exponencial (100ms, 200ms, 400ms, 800ms)
        assert_eq!(first_interval, Duration::from_millis(100));
        assert_eq!(second_interval, Duration::from_millis(200));
        assert_eq!(third_interval, Duration::from_millis(400));
        assert_eq!(fourth_interval, Duration::from_millis(800));

        // Verificação estrita de duplicação: se a mutação alterar * 2 para + 1, as asserções abaixo falharão
        assert_eq!(second_interval, first_interval * 2);
        assert_eq!(third_interval, second_interval * 2);
        assert_eq!(fourth_interval, third_interval * 2);

        let _shutdown_result = command_sender.send(NetworkCommand::Shutdown).await;
        let _manager_join_result = manager_task_handle.await;
    }

    #[tokio::test(start_paused = true)]
    async fn no_sleep_after_final_retry_attempt() {
        struct FailingTransport;
        #[async_trait::async_trait]
        impl P2pTransport for FailingTransport {
            fn local_id(&self) -> PeerId {
                make_peer("local")
            }
            fn local_addr(&self) -> Result<PeerAddr, ConnectionError> {
                Err(ConnectionError::Shutdown)
            }
            async fn accept(
                &self,
            ) -> Result<Box<dyn crate::core::transport::IncomingConnection>, ConnectionError>
            {
                std::future::pending().await
            }
            async fn open_bi(
                &self, _alpn: &[u8], _peer: &PeerAddr,
            ) -> Result<
                (
                    Box<dyn tokio::io::AsyncWrite + Send + Unpin>,
                    Box<dyn tokio::io::AsyncRead + Send + Unpin>,
                ),
                ConnectionError,
            > {
                Err(ConnectionError::StreamFailed("simulated error".to_string()))
            }
            async fn latency(&self, _peer: &PeerId) -> Option<Duration> {
                None
            }
            async fn shutdown(&self) -> Result<(), ConnectionError> {
                Ok(())
            }
        }

        let transport = Arc::new(FailingTransport);

        let (mut network_manager, command_sender, network_state) =
            NetworkManager::new(transport, open_validator(), no_op_emitter());

        network_manager.register_outbound(b"acerola/test/1", Arc::new(NoopHandler));
        let manager_task_handle = tokio::spawn(network_manager.run());

        command_sender
            .send(NetworkCommand::Connect {
                addr: PeerAddr { id: make_peer("target"), addrs: vec![] },
                alpn: b"acerola/test/1".to_vec(),
            })
            .await
            .unwrap();

        // Advance virtual time by 1500ms (100 + 200 + 400 + 800ms retry backoffs)
        tokio::time::sleep(Duration::from_millis(1500)).await;
        tokio::task::yield_now().await;

        // At 1500ms, all 5 attempts have failed.
        // If attempt < max_retries was used, no sleep occurred after the 5th attempt and the task finished,
        // reducing network_state strong_count back to 2 (manager + test body).
        // If attempt <= max_retries was used, the task is currently sleeping for 1600ms, keeping strong_count at 3.
        assert_eq!(
            Arc::strong_count(&network_state),
            2,
            "Outbound retry task should have completed without an extra sleep after the 5th attempt"
        );

        let _ = command_sender.send(NetworkCommand::Shutdown).await;
        let _ = manager_task_handle.await;
    }
}
