use acerola_p2p::api::AcerolaP2p;
use std::sync::Arc;
use tokio::runtime::Runtime;

#[cfg(target_os = "android")]
use acerola_p2p::api::{
    guard::{InMemoryTrustedStore, TofuGuard, TrustedPeerStore},
    identity::{DefaultDeviceInfoProvider, DeviceInfoProvider},
    network::NetworkMode,
    transport::IrohTransportBuilder,
};

#[cfg(target_os = "android")]
use crate::{mode::FfiNetworkMode, singleton::TOKIO_RUNTIME};

#[cfg(target_os = "android")]
use std::collections::HashMap;

#[uniffi::export(with_foreign)]
pub trait P2PCallback: Send + Sync {
    fn on_event(&self, event: String, data: String);
}

#[derive(uniffi::Object)]
pub struct P2PNode {
    node: Arc<AcerolaP2p>,
    runtime: Arc<Runtime>,
}

#[uniffi::export]
#[cfg(target_os = "android")]
impl P2PNode {
    #[uniffi::constructor]
    pub fn new(callback: Arc<dyn P2PCallback>) -> Self {
        let runtime = TOKIO_RUNTIME.clone();

        let node = runtime.block_on(async {
            let cb_clone = Arc::clone(&callback);

            let emit: acerola_p2p::api::protocol::EventEmitter = Arc::new(move |event, data| {
                cb_clone.on_event(event.to_string(), data);
            });

            let store = Arc::new(InMemoryTrustedStore::new());

            let transport = IrohTransportBuilder::default()
                .relay("https://use1-1.relay.iroh.network/");

            // TODO: Fazer isso mudar junto com a versão do app android, colocar na action de release.
            let device = DefaultDeviceInfoProvider::new("Android", "0.2.2-beta")
                .provide()
                .expect("Failed to read device info");
 
            Arc::new(
                AcerolaP2p::builder(emit, transport, device)
                    .guard(
                        TofuGuard::new(Arc::clone(&store) as Arc<dyn TrustedPeerStore>).into_validator(),
                    )
                    .build()
                    .await
                    .expect("Failed to start P2P node"),
            )
        });

        Self { node, runtime }
    }

    pub fn get_local_id(&self) -> String {
        self.node.local_id().to_string()
    }

    pub fn connect(&self, peer_id: String, alpn: Vec<u8>) {
        let node = Arc::clone(&self.node);
        self.runtime.spawn(async move {
            let _ = node.connect(&peer_id, &alpn).await;
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
        self.runtime.spawn(async move {
            let store = Arc::new(InMemoryTrustedStore::new());
            let guard = TofuGuard::new(store as Arc<dyn TrustedPeerStore>).into_validator();
            let _ = node.switch_guard(guard, NetworkMode::Local).await;
        });
    }

    pub fn switch_to_relay(&self) {
        let node = Arc::clone(&self.node);
        self.runtime.spawn(async move {
            let store = Arc::new(InMemoryTrustedStore::new());
            let guard = TofuGuard::new(store as Arc<dyn TrustedPeerStore>).into_validator();
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
}
