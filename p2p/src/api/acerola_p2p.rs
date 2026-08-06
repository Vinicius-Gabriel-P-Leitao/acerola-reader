use std::{
    collections::{HashMap, HashSet},
    sync::Arc,
};

use tokio::sync::{mpsc, RwLock};

use super::acerola_builder::AcerolaP2pBuilder;
use crate::{
    core::{
        guard::BoxedValidator,
        network::{
            manager::NetworkCommand,
            state::{NetworkMode, NetworkState},
        },
        transport::TransportP2pBuilder,
    },
    data::{identity::device_info::DeviceInfo, protocol::EventEmitter},
    infra::{
        error::ConnectionError,
        peer::{PeerAddr, PeerId},
    },
};

/// A Instância consolidada e o Controlador do Nó rodando em background.
///
/// É com este objeto instanciado que a aplicação cliente irá de fato provocar a rede,
/// discando ativamente para outros nós, requisitando o estado global ou mandando
/// comandos administrativos de desligamento ou reorientação (SwitchGuard).
pub struct AcerolaP2p {
    pub(super) command_tx: mpsc::Sender<NetworkCommand>,
    pub(super) state: Arc<RwLock<NetworkState>>,
    pub(super) device_info: DeviceInfo,
    pub(super) local_addr: PeerAddr,
    pub(super) local_id: PeerId,
}

impl AcerolaP2p {
    /// Único ponto de partida da API, devolve uma estrutura passível de configuração.
    pub fn builder<TB: TransportP2pBuilder>(
        emit: EventEmitter, transport: TB, device_info: DeviceInfo,
    ) -> AcerolaP2pBuilder<TB>
    where
        TB::Output: 'static,
    {
        AcerolaP2pBuilder::new(emit, transport, device_info)
    }

    /// Retorna a string legível (Base32/Base64, dependendo do backend Iroh) do nó residente.
    pub fn local_id(&self) -> &str {
        &self.local_id.id
    }

    /// Retorna um objeto do PeerAddr, juntamente vem o id do peerid + addr
    pub fn local_addr(&self) -> &PeerAddr {
        &self.local_addr
    }

    /// Retorna o `device_id` UUID v5 derivado da chave pública do nó.
    pub fn local_device_id(&self) -> Option<&str> {
        self.local_id.device_id.as_deref()
    }

    /// Retorna os metadados do dispositivo local informados no builder.
    pub fn local_device_info(&self) -> &DeviceInfo {
        &self.device_info
    }

    /// Pede ativamente ao daemon de gerência para abrir um pipe QUIC até um determinado Nó.
    ///
    /// Se a resposta for exitosa, as transmissões vão direto pro handler do protocolo (`alpn`) mapeado.
    #[rustfmt::skip]
    pub async fn connect(&self, addr: PeerAddr, alpn: &[u8]) -> Result<(), ConnectionError> {
        self.command_tx.send(NetworkCommand::Connect { addr, alpn: alpn.to_vec() }).await.map_err(|_| ConnectionError::Shutdown)
    }

    /// Captura um Snapshot/Cópia pesada dos nós que estão trafegando e seus marcadores de sub-protocolo atrelados.
    pub async fn connected_peers(&self) -> HashMap<PeerId, HashSet<Vec<u8>>> {
        self.state.read().await.peers().clone()
    }

    /// Captura peers conectados junto com suas informações de dispositivo, em lock único.
    pub async fn connected_peers_with_info(
        &self,
    ) -> Vec<(PeerId, HashSet<Vec<u8>>, Option<DeviceInfo>)> {
        let state = self.state.read().await;
        state
            .peers()
            .iter()
            .map(|(peer, alpns)| {
                (peer.clone(), alpns.clone(), state.get_device_info(peer).cloned())
            })
            .collect()
    }

    /// Extrai o modo da interface (Local/Relay) operando no momento.
    pub async fn mode(&self) -> NetworkMode {
        self.state.read().await.mode().clone()
    }

    /// Solicita em runtime ao Daemon a permuta de middleware de restrição sem cair nenhuma thread.
    ///
    /// Ao receber o novo validator o gerente recusa/aceita instantes novas conexões sem derrubar os sockets mantidos.
    #[rustfmt::skip]
    pub async fn switch_guard(
        &self, validator: BoxedValidator, mode: NetworkMode,
    ) -> Result<(), ConnectionError> {
        self.command_tx.send(NetworkCommand::SwitchGuard { validator, mode }).await.map_err(|_| ConnectionError::Shutdown)
    }

    /// Aciona a sequência final de drenagem forçando o cancelamento do background Event Loop
    /// e do serviço nativo na memória.
    pub async fn shutdown(&self) -> Result<(), ConnectionError> {
        self.command_tx.send(NetworkCommand::Shutdown).await.map_err(|_| ConnectionError::Shutdown)
    }
}

#[cfg(all(test, feature = "iroh"))]
mod tests {
    use std::sync::Mutex;

    use super::*;
    use crate::{
        core::transport::iroh::IrohTransportBuilder, data::identity::device_info::DeviceInfo,
    };

    fn no_op_emitter() -> EventEmitter {
        Arc::new(|_event: &str, _payload: String| {})
    }

    fn capture_emitter() -> (EventEmitter, Arc<Mutex<Vec<String>>>) {
        let events = Arc::new(Mutex::new(Vec::new()));
        let clone = Arc::clone(&events);
        let emit: EventEmitter = Arc::new(move |event: &str, _: String| {
            clone.lock().unwrap().push(event.to_string());
        });
        (emit, events)
    }

    fn test_device_info() -> DeviceInfo {
        DeviceInfo {
            name: "test-device".to_string(),
            os: "linux".to_string(),
            version: "0.0.1".to_string(),
        }
    }

    async fn build_node() -> AcerolaP2p {
        AcerolaP2p::builder(no_op_emitter(), IrohTransportBuilder::default(), test_device_info())
            .build()
            .await
            .unwrap()
    }

    #[tokio::test]
    async fn local_id_not_empty() {
        let node = build_node().await;
        assert!(!node.local_id().is_empty());
    }

    #[tokio::test]
    async fn connected_peers_start_empty() {
        let node = build_node().await;
        assert!(node.connected_peers().await.is_empty());
    }

    #[tokio::test]
    async fn shutdown_without_error() {
        let node = build_node().await;
        assert!(node.shutdown().await.is_ok());
    }

    #[tokio::test]
    async fn initial_mode_is_local() {
        let node = build_node().await;
        assert_eq!(node.mode().await, NetworkMode::Local);
    }

    #[tokio::test]
    async fn two_nodes_have_distinct_ids() {
        let (emit_a, _) = capture_emitter();
        let (emit_b, _) = capture_emitter();

        let node_a =
            AcerolaP2p::builder(emit_a, IrohTransportBuilder::default(), test_device_info())
                .build()
                .await
                .unwrap();

        let node_b =
            AcerolaP2p::builder(emit_b, IrohTransportBuilder::default(), test_device_info())
                .build()
                .await
                .unwrap();

        assert_ne!(node_a.local_id(), node_b.local_id());
    }

    #[tokio::test]
    async fn local_device_id_populated() {
        let node = build_node().await;
        assert!(node.local_device_id().is_some());
    }

    #[tokio::test]
    async fn same_seed_generates_same_device_id() {
        let seed = [0x42u8; 32];

        let node_a = AcerolaP2p::builder(
            no_op_emitter(),
            IrohTransportBuilder::default().seed(seed),
            test_device_info(),
        )
        .build()
        .await
        .unwrap();

        let node_b = AcerolaP2p::builder(
            no_op_emitter(),
            IrohTransportBuilder::default().seed(seed),
            test_device_info(),
        )
        .build()
        .await
        .unwrap();

        assert_eq!(node_a.local_device_id(), node_b.local_device_id());
    }

    #[tokio::test]
    async fn different_seeds_generate_different_device_ids() {
        let node_a = AcerolaP2p::builder(
            no_op_emitter(),
            IrohTransportBuilder::default().seed([0x11u8; 32]),
            test_device_info(),
        )
        .build()
        .await
        .unwrap();

        let node_b = AcerolaP2p::builder(
            no_op_emitter(),
            IrohTransportBuilder::default().seed([0x22u8; 32]),
            test_device_info(),
        )
        .build()
        .await
        .unwrap();

        assert_ne!(node_a.local_device_id(), node_b.local_device_id());
    }

    #[tokio::test]
    async fn device_info_name_accessible_after_build() {
        let info = DeviceInfo {
            name: "meu-pc".to_string(),
            os: "linux".to_string(),
            version: "1.0.0".to_string(),
        };
        let node = AcerolaP2p::builder(no_op_emitter(), IrohTransportBuilder::default(), info)
            .build()
            .await
            .unwrap();
        assert_eq!(node.local_device_info().name, "meu-pc");
    }

    #[tokio::test]
    async fn device_info_os_accessible_after_build() {
        let info = DeviceInfo {
            name: "meu-pc".to_string(),
            os: "windows".to_string(),
            version: "1.0.0".to_string(),
        };
        let node = AcerolaP2p::builder(no_op_emitter(), IrohTransportBuilder::default(), info)
            .build()
            .await
            .unwrap();
        assert_eq!(node.local_device_info().os, "windows");
    }

    #[tokio::test]
    async fn device_info_version_accessible_after_build() {
        let info = DeviceInfo {
            name: "meu-pc".to_string(),
            os: "linux".to_string(),
            version: "2.3.1".to_string(),
        };
        let node = AcerolaP2p::builder(no_op_emitter(), IrohTransportBuilder::default(), info)
            .build()
            .await
            .unwrap();
        assert_eq!(node.local_device_info().version, "2.3.1");
    }

    #[tokio::test]
    async fn two_nodes_with_distinct_device_infos_are_independent() {
        let (emit_a, _) = capture_emitter();
        let (emit_b, _) = capture_emitter();

        let node_a = AcerolaP2p::builder(
            emit_a,
            IrohTransportBuilder::default(),
            DeviceInfo {
                name: "desktop".to_string(),
                os: "linux".to_string(),
                version: "1.0.0".to_string(),
            },
        )
        .build()
        .await
        .unwrap();

        let node_b = AcerolaP2p::builder(
            emit_b,
            IrohTransportBuilder::default(),
            DeviceInfo {
                name: "android".to_string(),
                os: "android".to_string(),
                version: "1.0.0".to_string(),
            },
        )
        .build()
        .await
        .unwrap();

        assert_ne!(node_a.local_device_info().name, node_b.local_device_info().name);
        assert_ne!(node_a.local_device_info().os, node_b.local_device_info().os);
    }

    #[tokio::test]
    async fn connected_peers_with_info_returns_peers_and_device_info() {
        let node = build_node().await;
        let peer = PeerId { id: "test-connected-peer".to_string(), device_id: None };
        let addr = PeerAddr { id: peer.clone(), addrs: vec![] };
        let device_info = test_device_info();

        {
            let mut state_guard = node.state.write().await;
            state_guard.connect(peer.clone(), addr, b"acerola/handshake/1".to_vec());
            state_guard.store_device_info(peer.clone(), device_info.clone());
        }

        let peers_with_info = node.connected_peers_with_info().await;
        assert_eq!(peers_with_info.len(), 1);
        assert_eq!(peers_with_info[0].0, peer);
        assert!(peers_with_info[0].1.contains(b"acerola/handshake/1".as_slice()));
        assert_eq!(peers_with_info[0].2, Some(device_info));
    }

    #[tokio::test]
    async fn switch_guard_command_updates_network_mode_and_returns_ok() {
        let node = build_node().await;
        let deny_validator: BoxedValidator = Box::new(|_ctx| {
            Box::pin(async { Err(ConnectionError::AuthDenied("switch test".into())) })
        });

        let switch_result = node.switch_guard(deny_validator, NetworkMode::Relay).await;
        assert!(switch_result.is_ok());

        tokio::time::sleep(std::time::Duration::from_millis(50)).await;
        assert_eq!(node.mode().await, NetworkMode::Relay);
    }

    #[tokio::test]
    async fn shutdown_command_sends_shutdown_signal_and_returns_ok() {
        let node = build_node().await;
        let shutdown_result = node.shutdown().await;
        assert!(shutdown_result.is_ok());

        // Após o shutdown, tentar enviar novos comandos pelo canal command_tx deve falhar com erro
        tokio::time::sleep(std::time::Duration::from_millis(50)).await;
        let peer_address = PeerAddr {
            id: PeerId { id: "remote".to_string(), device_id: None },
            addrs: vec![],
        };
        let connect_result = node.connect(peer_address, b"acerola/handshake/1").await;
        assert!(connect_result.is_err());
    }
}
