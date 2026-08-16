use acerola_p2p::api::AcerolaP2p;
use std::sync::Arc;
use tokio::runtime::Runtime;

#[cfg(target_os = "android")]
use acerola_p2p::api::{
    guard::TofuGuard,
    identity::{DefaultDeviceInfoProvider, DeviceInfoProvider},
    network::NetworkMode,
    peer::{PeerAddr, PeerIdentity},
    storage::P2PStorage,
    transport::IrohTransportBuilder,
};

#[cfg(target_os = "android")]
use crate::{
    callbacks::{FileSyncProvider, HistorySyncProvider, SecureBlobStore},
    mode::FfiNetworkMode,
    protocol::{
        files::{FileSyncInbound, FileSyncOutbound, FILE_SYNC_ALPN},
        history::{HistorySyncInbound, HistorySyncOutbound, HISTORY_SYNC_ALPN},
    },
    singleton::TOKIO_RUNTIME,
    storage::{SecureP2pStorage, SharedSecureP2pStorage},
    trust_store::SecureTrustedStore,
};

#[cfg(target_os = "android")]
use std::collections::HashMap;

/// URL do relay oficial do projeto, usado quando nenhum override é fornecido pelo app.
///
/// TODO: Fazer isso mudar junto com a versão do app android, colocar na action de release.
#[cfg(target_os = "android")]
const DEFAULT_RELAY_URL: &str = "https://relay.acerola-comic.com/";

#[uniffi::export(with_foreign)]
pub trait P2PCallback: Send + Sync {
    fn on_event(&self, event: String, data: String);
}

#[derive(uniffi::Record)]
pub struct FfiPeerAddr {
    pub id: String,
    pub device_id: Option<String>,
    pub addrs: Vec<u8>,
}

#[derive(uniffi::Record)]
pub struct FfiConnectedPeer {
    pub peer_id: String,
    pub alpns: Vec<Vec<u8>>,
    pub device_name: Option<String>,
}

#[derive(uniffi::Object)]
pub struct P2PNode {
    node: Arc<AcerolaP2p>,
    runtime: Arc<Runtime>,
    #[cfg(target_os = "android")]
    trust_store: Arc<SecureTrustedStore>,
    #[cfg(target_os = "android")]
    storage: Arc<SecureP2pStorage>,
}

#[uniffi::export]
#[cfg(target_os = "android")]
impl P2PNode {
    #[uniffi::constructor]
    pub fn new(
        callback: Arc<dyn P2PCallback>, legacy_data_dir: Option<String>, relay_url: Option<String>,
        device_name: String, device_version: String, secure_store: Arc<dyn SecureBlobStore>,
        history_provider: Arc<dyn HistorySyncProvider>, file_provider: Arc<dyn FileSyncProvider>,
    ) -> Self {
        let runtime = TOKIO_RUNTIME.clone();

        let cb_clone = Arc::clone(&callback);
        let emit: acerola_p2p::api::protocol::EventEmitter = Arc::new(move |event, data| {
            cb_clone.on_event(event.to_string(), data);
        });

        // Só usado pra migrar `identity.seed`/`peers.json`/`trusted.txt`/`blocked.txt` (formato
        // antigo, texto puro, de antes do `SecureBlobStore`) — ver `storage.rs`/`trust_store.rs`.
        let legacy_dir = legacy_data_dir.as_deref().map(std::path::Path::new);

        let trust_store = Arc::new(SecureTrustedStore::open(
            Arc::clone(&secure_store),
            Arc::clone(&emit),
            legacy_dir,
        ));

        let storage = Arc::new(SecureP2pStorage::open(Arc::clone(&secure_store), legacy_dir));

        let node = {
            let trust_store = Arc::clone(&trust_store);
            let storage_for_builder = Arc::clone(&storage);
            let emit_for_handlers = Arc::clone(&emit);
            runtime.block_on(async move {
                let transport = IrohTransportBuilder::default()
                    .relay(relay_url.as_deref().unwrap_or(DEFAULT_RELAY_URL));

                let device = DefaultDeviceInfoProvider::new(device_name, device_version)
                    .provide()
                    .expect("Failed to read device info");

                Arc::new(
                    AcerolaP2p::builder(emit, transport, device)
                        .guard(
                            TofuGuard::new(trust_store as Arc<dyn acerola_p2p::api::guard::TrustedPeerStore>)
                                .into_validator(),
                        )
                        .storage(SharedSecureP2pStorage(storage_for_builder))
                        .inbound(
                            HISTORY_SYNC_ALPN,
                            Arc::new(HistorySyncInbound::new(
                                Arc::clone(&emit_for_handlers),
                                Arc::clone(&history_provider),
                            )),
                        )
                        .outbound(
                            HISTORY_SYNC_ALPN,
                            Arc::new(HistorySyncOutbound::new(
                                Arc::clone(&emit_for_handlers),
                                history_provider,
                            )),
                        )
                        .inbound(
                            FILE_SYNC_ALPN,
                            Arc::new(FileSyncInbound::new(Arc::clone(&emit_for_handlers), Arc::clone(&file_provider))),
                        )
                        .outbound(FILE_SYNC_ALPN, Arc::new(FileSyncOutbound::new(emit_for_handlers, file_provider)))
                        .build()
                        .await
                        .expect("Failed to start P2P node"),
                )
            })
        };

        Self { node, runtime, trust_store, storage }
    }

    pub fn get_local_id(&self) -> String {
        self.node.local_id().to_string()
    }

    pub fn get_local_addr(&self) -> FfiPeerAddr {
        let addr = self.node.local_addr();
        FfiPeerAddr {
            id: addr.id.id.clone(),
            device_id: addr.id.device_id.clone(),
            addrs: addr.addrs.clone(),
        }
    }

    pub fn connect(&self, peer_addr: FfiPeerAddr, alpn: Vec<u8>) {
        let node = Arc::clone(&self.node);
        let addr = PeerAddr {
            id: PeerIdentity {
                id: peer_addr.id,
                device_id: peer_addr.device_id,
            },
            addrs: peer_addr.addrs,
        };
        self.runtime.spawn(async move {
            let _ = node.connect(addr, &alpn).await;
        });
    }

    pub fn shutdown(&self) {
        let node = Arc::clone(&self.node);
        self.runtime.block_on(async move {
            let _ = node.shutdown().await;
        });
    }

    pub fn switch_to_local(&self) {
        let node = Arc::clone(&self.node);
        let trust_store = Arc::clone(&self.trust_store);
        self.runtime.spawn(async move {
            let guard = TofuGuard::new(trust_store as Arc<dyn acerola_p2p::api::guard::TrustedPeerStore>)
                .into_validator();
            let _ = node.switch_guard(guard, NetworkMode::Local).await;
        });
    }

    pub fn switch_to_relay(&self) {
        let node = Arc::clone(&self.node);
        let trust_store = Arc::clone(&self.trust_store);
        self.runtime.spawn(async move {
            let guard = TofuGuard::new(trust_store as Arc<dyn acerola_p2p::api::guard::TrustedPeerStore>)
                .into_validator();
            let _ = node.switch_guard(guard, NetworkMode::Relay).await;
        });
    }

    pub fn get_mode(&self) -> FfiNetworkMode {
        self.runtime
            .block_on(async { self.node.mode().await.into() })
    }

    pub fn get_connected_peers(&self) -> HashMap<String, Vec<Vec<u8>>> {
        self.runtime.block_on(async {
            self.node
                .connected_peers()
                .await
                .into_iter()
                .map(|(peer, alpns)| (peer.id.clone(), alpns.into_iter().collect()))
                .collect()
        })
    }

    /// Devolve todos os peers já pareados (TOFU) em algum momento, com o último endereço
    /// conhecido — persistem entre reinícios do app e independem de conexão ativa no momento,
    /// já que a conexão de handshake em si dura só alguns segundos (troca PING/PONG/DeviceInfo
    /// e fecha). É essa lista, não `get_connected_peers*`, que deve alimentar "dispositivos
    /// pareados" na UI.
    pub fn get_paired_peers(&self) -> Vec<FfiPeerAddr> {
        let storage = Arc::clone(&self.storage);
        self.runtime.block_on(async move {
            storage
                .load_peers()
                .await
                .unwrap_or_default()
                .into_iter()
                .map(|addr| FfiPeerAddr {
                    id: addr.id.id,
                    device_id: addr.id.device_id,
                    addrs: addr.addrs,
                })
                .collect()
        })
    }

    pub fn get_connected_peers_with_info(&self) -> Vec<FfiConnectedPeer> {
        self.runtime.block_on(async {
            self.node
                .connected_peers_with_info()
                .await
                .into_iter()
                .map(|(peer, alpns, device)| FfiConnectedPeer {
                    peer_id: peer.id.clone(),
                    alpns: alpns.into_iter().collect(),
                    device_name: device.map(|info| info.name),
                })
                .collect()
        })
    }
}
