use std::{collections::HashSet, sync::Arc};

use acerola_p2p::api::{
    guard::{InMemoryTrustedStore, TofuGuard, TrustedPeerStore},
    identity::DeviceInfo,
    network::NetworkMode,
    peer::{PeerAddr, PeerIdentity},
    storage::P2PStorage,
    AcerolaP2p,
};
use async_trait::async_trait;

pub type ConnectedPeerInfo = (PeerIdentity, HashSet<Vec<u8>>, Option<DeviceInfo>);

#[async_trait]
pub trait NetworkServiceApi: Send + Sync + 'static {
    fn local_id(&self) -> Result<String, String>;
    fn local_addr(&self) -> Result<PeerAddr, String>;
    fn local_device_info(&self) -> Result<DeviceInfo, String>;
    async fn connected_peers_with_info(&self) -> Result<Vec<ConnectedPeerInfo>, String>;
    /// Todo peer já pareado (TOFU) alguma vez, com o último endereço conhecido — persiste
    /// entre reinícios e independe de conexão ativa agora, diferente de
    /// [`NetworkServiceApi::connected_peers_with_info`] (sessão de protocolo, dura só
    /// segundos). É essa lista que deve alimentar "disparar sync com X" na UI, já que o peer
    /// quase nunca está conectado no exato instante em que o usuário clica o botão.
    async fn paired_peers(&self) -> Result<Vec<PeerAddr>, String>;
    async fn switch_to_local(&self) -> Result<(), String>;
    async fn switch_to_relay(&self) -> Result<(), String>;
    async fn mode(&self) -> Result<NetworkMode, String>;
    async fn connect(&self, peer_addr: PeerAddr, alpn: Vec<u8>) -> Result<(), String>;
    async fn shutdown(&self) -> Result<(), String>;
}

pub struct NetworkService {
    node: Arc<AcerolaP2p>,
    /// Mesmo storage passado ao builder (`.storage(...)`) — clonado antes por
    /// `bios::network::setup_network` pra sobreviver aqui como fonte de "peers pareados"
    /// persistidos, já que a lib não devolve o storage de volta depois do `build()`.
    storage: Arc<dyn P2PStorage>,
}

impl NetworkService {
    pub fn new(node: Arc<AcerolaP2p>, storage: Arc<dyn P2PStorage>) -> Self {
        Self { node, storage }
    }
}

#[async_trait]
impl NetworkServiceApi for NetworkService {
    fn local_id(&self) -> Result<String, String> {
        Ok(self.node.local_id().to_string())
    }

    /// Endereço completo (id + bytes de endereçamento) usado pra gerar o código/QR de
    /// pareamento — é o que o outro dispositivo precisa pra nos alcançar via `connect()`.
    fn local_addr(&self) -> Result<PeerAddr, String> {
        Ok(self.node.local_addr().clone())
    }

    /// Nome/OS/versão deste dispositivo — usado na tela de Rede pra exibir algo mais
    /// legível que o peer id cru (ex: "Notebook do Vinicius" em vez de um hex de 64 chars).
    fn local_device_info(&self) -> Result<DeviceInfo, String> {
        Ok(self.node.local_device_info().clone())
    }

    async fn connected_peers_with_info(&self) -> Result<Vec<ConnectedPeerInfo>, String> {
        Ok(self.node.connected_peers_with_info().await)
    }

    async fn paired_peers(&self) -> Result<Vec<PeerAddr>, String> {
        self.storage.load_peers().await.map_err(|err| err.to_string())
    }

    async fn switch_to_local(&self) -> Result<(), String> {
        let store = Arc::new(InMemoryTrustedStore::new());
        let guard = TofuGuard::new(store as Arc<dyn TrustedPeerStore>).into_validator();
        self.node.switch_guard(guard, NetworkMode::Local).await.map_err(|err| err.to_string())
    }

    async fn switch_to_relay(&self) -> Result<(), String> {
        let store = Arc::new(InMemoryTrustedStore::new());
        let guard = TofuGuard::new(store as Arc<dyn TrustedPeerStore>).into_validator();
        self.node.switch_guard(guard, NetworkMode::Relay).await.map_err(|err| err.to_string())
    }

    async fn mode(&self) -> Result<NetworkMode, String> {
        Ok(self.node.mode().await)
    }

    async fn connect(&self, peer_addr: PeerAddr, alpn: Vec<u8>) -> Result<(), String> {
        self.node.connect(peer_addr, &alpn).await.map_err(|err| err.to_string())
    }

    async fn shutdown(&self) -> Result<(), String> {
        self.node.shutdown().await.map_err(|err| err.to_string())
    }
}
