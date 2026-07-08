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
        transport::P2pTransport,
    },
    data::protocol::{rpc::GOODBYE, ProtocolHandler},
    infra::{error::ConnectionError, peer::PeerAddr},
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
    /// Fila para consumo dos comandos requisitados externamente.
    command_rx: mpsc::Receiver<NetworkCommand>,
    /// Tabela de protocolos autorizados para quem recebe conexões (Servidor).
    handlers_inbound: HashMap<Vec<u8>, Arc<dyn ProtocolHandler>>,
    /// Tabela de protocolos operados por quem inicia conexões (Cliente).
    handlers_outbound: HashMap<Vec<u8>, Arc<dyn ProtocolHandler>>,
}

impl NetworkManager {
    /// Inicializa os componentes internos de gerência de rede.
    ///
    /// Cria e compartilha buffers MPSC e o `NetworkState`. Retorna uma tupla
    /// contendo a instância pronta para rodar, o comunicador (sender) e a view
    /// do estado da rede, garantindo que o chamador mantenha as referências ativas.
    pub fn new(
        transport: Arc<dyn P2pTransport>, validator: BoxedValidator,
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
        loop {
            tokio::select! {
                // Evento 1: Uma conexão externa solicitando handshake foi recebida.
                result = self.transport.accept() => {
                    match result {
                        Ok(incoming) => {
                            // Ignora e droppa conexões se o ALPN não está suportado no mapa local.
                            let Some(handler) = self.handlers_inbound.get(incoming.alpn()) else { continue };

                            let state = Arc::clone(&self.state);
                            let handler = handler.clone();
                            let validator = Arc::clone(&self.validator);

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

                            let span = tracing::info_span!(
                                "outbound",
                                addr = %addr.id,
                                alpn = ?String::from_utf8_lossy(&alpn)
                            );

                            // Procede abrindo requisição ativa à interface física.
                            match self.transport.open_bi(&alpn, &addr).await {
                                Ok((send, recv)) => {
                                    state.write().await.connect(addr_clone.id.clone(), addr_clone.clone(), alpn_clone.clone());
                                    tracing::debug!(parent: &span, "outbound connection established");

                                    tokio::spawn(async move {
                                        if let Err(err) = handler.handle(&addr_clone.id, send, recv).await {
                                            tracing::warn!(error = ?err, "outbound handler failed");
                                        }

                                        tracing::debug!("outbound connection closed");
                                        state.write().await.disconnect(&addr_clone.id, &alpn_clone);
                                    }.instrument(span));
                                }
                                Err(err) => {
                                    tracing::warn!(parent: &span, error = ?err, "connect failed");
                                }
                            }
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

#[cfg(test)]
mod tests {
    use tokio::time::{sleep, Duration};

    use super::*;
    use crate::{infra::peer::PeerId, tests::mock_transport::mock_transport};

    fn open_validator() -> BoxedValidator {
        Box::new(|_ctx| Box::pin(async { Ok(()) }))
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
    async fn handler_inbound_registrado_para_alpn_e_encontrado() {
        let (transport, _handle) = mock_transport();
        let (mut manager, _, _) = NetworkManager::new(Arc::new(transport), open_validator());
        manager.register_inbound(b"acerola/handshake/1", Arc::new(NoopHandler));
        assert!(manager.handlers_inbound.contains_key(b"acerola/handshake/1".as_ref()));
    }

    #[tokio::test]
    async fn handler_outbound_registrado_para_alpn_e_encontrado() {
        let (transport, _handle) = mock_transport();
        let (mut manager, _, _) = NetworkManager::new(Arc::new(transport), open_validator());
        manager.register_outbound(b"acerola/handshake/1", Arc::new(NoopHandler));
        assert!(manager.handlers_outbound.contains_key(b"acerola/handshake/1".as_ref()));
    }

    #[tokio::test]
    async fn peer_adicionado_ao_state_ao_aceitar_conexao() {
        let (transport, handle) = mock_transport();
        let transport: Arc<dyn P2pTransport> = Arc::new(transport);
        let (mut manager, _, state) = NetworkManager::new(Arc::clone(&transport), open_validator());
        manager.register_inbound(b"acerola/handshake/1", Arc::new(SlowHandler));

        let (client, server) = tokio::io::duplex(1024);
        handle.inject(b"acerola/handshake/1", make_peer("peer-1"), client, server);

        tokio::spawn(manager.run());
        sleep(Duration::from_millis(20)).await;

        assert!(state.read().await.is_connected(&make_peer("peer-1")));
    }

    #[tokio::test]
    async fn peer_removido_do_state_quando_handler_termina() {
        let (transport, handle) = mock_transport();
        let transport: Arc<dyn P2pTransport> = Arc::new(transport);
        let (mut manager, _, state) = NetworkManager::new(Arc::clone(&transport), open_validator());
        manager.register_inbound(b"acerola/handshake/1", Arc::new(NoopHandler));

        let (client, server) = tokio::io::duplex(1024);
        handle.inject(b"acerola/handshake/1", make_peer("peer-2"), client, server);

        tokio::spawn(manager.run());
        sleep(Duration::from_millis(50)).await;

        assert!(!state.read().await.is_connected(&make_peer("peer-2")));
    }

    #[tokio::test]
    async fn alpn_desconhecido_e_ignorado() {
        let (transport, handle) = mock_transport();
        let transport: Arc<dyn P2pTransport> = Arc::new(transport);
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
        let _ = command_tx.send(NetworkCommand::Shutdown).await;

        let result = tokio::time::timeout(Duration::from_millis(100), handle).await;
        assert!(result.is_ok());
    }

    #[tokio::test]
    async fn guard_nega_conexao_de_peer_bloqueado() {
        let (transport, handle) = mock_transport();
        let transport: Arc<dyn P2pTransport> = Arc::new(transport);

        let deny_all: BoxedValidator = Box::new(|_ctx| {
            Box::pin(async { Err(ConnectionError::AuthDenied("test deny all".into())) })
        });

        let (mut manager, _, state) = NetworkManager::new(Arc::clone(&transport), deny_all);
        manager.register_inbound(b"acerola/handshake/1", Arc::new(SlowHandler));

        let (client, server) = tokio::io::duplex(1024);
        handle.inject(b"acerola/handshake/1", make_peer("peer-blocked"), client, server);

        tokio::spawn(manager.run());
        sleep(Duration::from_millis(30)).await;

        assert!(!state.read().await.is_connected(&make_peer("peer-blocked")));
    }

    #[tokio::test]
    async fn mesmo_peer_em_dois_alpns_aparece_conectado() {
        let (transport, handle) = mock_transport();
        let transport: Arc<dyn P2pTransport> = Arc::new(transport);
        let (mut manager, _, state) = NetworkManager::new(Arc::clone(&transport), open_validator());

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
}
