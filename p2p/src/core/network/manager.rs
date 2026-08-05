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
    data::protocol::{rpc::GOODBYE, EventEmitter, ProtocolHandler},
    infra::{
        error::ConnectionError,
        peer::{PeerAddr, PeerId},
    },
};

/// Limite de comandos simultâneos não processados na fila do loop principal.
const COMMAND_CHANNEL_CAPACITY: usize = 64;

/// Sinais de controle enviados ao Event Loop da rede.
///
/// Como o manager é blindado e opera numa tarefa em background, toda interação
/// externa é sinalizada e enfileirada no canal por meio deste enum.
pub enum NetworkCommand {
    /// Troca dinâmica da política de validação (Guard) e estado nominal da rede.
    SwitchGuard { validator: BoxedValidator, mode: NetworkMode },
    /// Tenta discar ativamente para outro par através de um protocolo.
    Connect { addr: PeerAddr, alpn: Vec<u8> },
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
                // Evento 0: Checagem periódica da latência de todos os peers conectados.
                _ = latency_interval.tick() => {
                    let peers: Vec<PeerId> = self.state.read().await.peers().keys().cloned().collect();
                    for peer in peers {
                        if let Some(latency) = self.transport.latency(&peer).await {
                            let payload = serde_json::json!({
                                "peer_id": peer.id,
                                "latency_ms": latency.as_millis() as u64,
                            }).to_string();
                            (self.emit)("network:latency", payload);
                        }
                    }
                }
                // Evento 1: Uma conexão externa solicitando handshake foi recebida.
                result = self.transport.accept() => {
                    match result {
                        Ok(incoming) => {
                            // Ignora e droppa conexões se o ALPN não está suportado no mapa local.
                            let Some(handler) = self.handlers_inbound.get(incoming.alpn()) else { continue };

                            let state = Arc::clone(&self.state);
                            let handler = handler.clone();
                            let validator = Arc::clone(&self.validator);
                            let storage = self.storage.clone();

                            // Lança o tratamento da stream em background (Tarefa paralela).
                            let span = tracing::info_span!(
                                "inbound",
                                peer = %incoming.peer().id,
                                alpn = ?String::from_utf8_lossy(incoming.alpn())
                            );

                            tokio::spawn(async move {
                                let peer = incoming.peer().clone();
                                let addr = incoming.addr().clone();
                                let alpn = incoming.alpn().to_vec();

                                // Exige a promoção da conexão à canais de leitura e escrita.
                                let Ok((send, recv)) = incoming.accept_bi().await else { return };

                                // Etapa de Segurança: Invoca a Validação (Guard).
                                let ctx = ConnectionContext { peer_id: peer.clone(), data: () };
                                let allowed = {
                                    let guard = validator.read().await;
                                    guard(&ctx)
                                }.await;

                                if let Err(err) = allowed {
                                    tracing::debug!(error = ?err, "connection denied by guard");
                                    return; // O early return mata as streams `send` e `recv`.
                                }

                                // Etapa de Storage: Salva o peer no storage se configurado.
                                if save_peer_if_present(storage.as_ref(), &addr).await.is_err() {
                                    tracing::error!(peer = %peer.id, "failed to save peer to storage, terminating connection");
                                    return;
                                }

                                // Promove a conexão a 'Conectada' no tracker central
                                state.write().await.connect(peer.clone(), addr, alpn.clone());
                                tracing::debug!("connection accepted");

                                // Bloqueia esta Task na execução do protocolo ALPN assinalado.
                                if let Err(err) = handler.handle(&peer, send, recv).await {
                                    tracing::warn!(error = ?err, "inbound handler failed");
                                }
                                tracing::debug!("connection closed");

                                // Protocolo encerrou, retira a tag ALPN do Estado Central.
                                state.write().await.disconnect(&peer, &alpn);
                            }.instrument(span));
                        }
                        Err(ConnectionError::Shutdown) => break, // Trata finalização programada.
                        Err(err) => {
                            tracing::debug!(error = ?err, "transport accept failed");
                            continue; // Permite que a rede tente se recuperar sob outros erros.
                        }
                    }
                }
                // Evento 2: Uma requisição na fila do manager vinda da própria API da biblioteca.
                Some(cmd) = self.command_rx.recv() => {
                    match cmd {
                        NetworkCommand::Connect { addr, alpn } => {
                            let Some(handler) = self.handlers_outbound.get(&alpn) else { continue };

                            let state = Arc::clone(&self.state);
                            let handler = handler.clone();
                            let addr_clone = addr.clone();
                            let alpn_clone = alpn.clone();
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
                                        match transport.open_bi(&alpn_clone, &addr_clone).await {
                                            Ok((send, recv)) => {
                                                if save_peer_if_present(storage.as_ref(), &addr_clone).await.is_err() {
                                                    tracing::error!(peer = %addr_clone.id, "failed to save outbound peer to storage, terminating connection");
                                                    return;
                                                }

                                                state.write().await.connect(
                                                    addr_clone.id.clone(),
                                                    addr_clone.clone(),
                                                    alpn_clone.clone(),
                                                );
                                                tracing::debug!("outbound connection established");

                                                if let Err(err) =
                                                    handler.handle(&addr_clone.id, send, recv).await
                                                {
                                                    tracing::warn!(
                                                        error = ?err,
                                                        "outbound handler failed"
                                                    );
                                                }

                                                tracing::debug!("outbound connection closed");
                                                state
                                                    .write()
                                                    .await
                                                    .disconnect(&addr_clone.id, &alpn_clone);
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
                                                    backoff = (backoff * 2)
                                                        .min(std::time::Duration::from_secs(5));
                                                }
                                            }
                                        }
                                    }
                                }
                                .instrument(span),
                            );
                        }
                        NetworkCommand::SwitchGuard { validator, mode } => {
                            // Substitui on-the-fly as proteções de rede e estado operacional.
                            *self.validator.write().await = validator;
                            self.state.write().await.switch_mode(mode);
                        }
                        NetworkCommand::Shutdown => {
                            self.broadcast_goodbye().await;
                            break; // Rompe o loop explicitamente.
                        }
                    }
                }
            }
        }
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

    use tokio::time::{sleep, Duration};

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
        let (mut manager, _, state) =
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
        let (mut manager, _, state) =
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
        let (manager, _, state) =
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

        let (mut manager, _, state) =
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
        let (mut manager, _, state) =
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
        let (mut manager, _, _state) =
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

        let (mut manager, _, state) = NetworkManager::with_storage(
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
}
