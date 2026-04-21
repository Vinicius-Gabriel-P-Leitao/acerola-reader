use acerola_p2p::api::{
    error::P2PError,
    guard::{open_guard, ConnectionContext, Guard},
    network::NetworkMode,
    AcerolaP2P,
};
use once_cell::sync::Lazy;
use std::{collections::HashMap, sync::Arc};
use tokio::{runtime::Runtime, sync::RwLock};

uniffi::setup_scaffolding!();

#[uniffi::export]
pub trait P2PCallback: Send + Sync {
    fn on_event(&self, event: String, data: String);
}

static TOKIO_RUNTIME: Lazy<Arc<Runtime>> =
    Lazy::new(|| Arc::new(Runtime::new().expect("Failed to create Tokio runtime")));

#[derive(uniffi::Enum, Clone, Debug, PartialEq, Eq)]
pub enum FfiNetworkMode {
    Local,
    Relay,
}

impl From<NetworkMode> for FfiNetworkMode {
    fn from(value: NetworkMode) -> Self {
        match value {
            NetworkMode::Local => FfiNetworkMode::Local,
            NetworkMode::Relay => FfiNetworkMode::Relay,
        }
    }
}

pub async fn token_guard<T>(_ctx: &ConnectionContext<T>) -> Result<(), P2PError> {
    Ok(())
}

#[derive(uniffi::Object)]
pub struct P2PNode {
    node: Arc<AcerolaP2P>,
    runtime: Arc<Runtime>,
    mode: Arc<RwLock<NetworkMode>>,
}

#[uniffi::export]
impl P2PNode {
    #[uniffi::constructor]
    pub fn new(callback: Arc<dyn P2PCallback>) -> Self {
        let runtime = TOKIO_RUNTIME.clone();

        let node = runtime.block_on(async {
            let emit: acerola_p2p::api::protocol::EventEmitter = Arc::new(move |event, data| {
                callback.on_event(event.to_string(), data);
            });

            Arc::new(
                AcerolaP2P::builder(emit)
                    .guard(Box::new(|ctx| Box::pin(open_guard(ctx))))
                    .build()
                    .await
                    .expect("Failed to start P2P node"),
            )
        });

        Self {
            node,
            runtime,
            mode: Arc::new(RwLock::new(NetworkMode::Local)),
        }
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
        let mode = Arc::clone(&self.mode);
        self.runtime.spawn(async move {
            let validator: Guard = Box::new(|ctx| Box::pin(open_guard(ctx)));
            if node
                .switch_guard(validator, NetworkMode::Local)
                .await
                .is_ok()
            {
                *mode.write().await = NetworkMode::Local;
            }
        });
    }

    pub fn switch_to_relay(&self) {
        let node = Arc::clone(&self.node);
        let mode = Arc::clone(&self.mode);
        self.runtime.spawn(async move {
            let validator: Guard = Box::new(|ctx| Box::pin(token_guard(ctx)));
            if node
                .switch_guard(validator, NetworkMode::Relay)
                .await
                .is_ok()
            {
                *mode.write().await = NetworkMode::Relay;
            }
        });
    }

    pub fn get_mode(&self) -> FfiNetworkMode {
        self.runtime
            .block_on(async { self.mode.read().await.clone().into() })
    }

    pub fn get_connected_peers(&self) -> HashMap<String, Vec<Vec<u8>>> {
        self.runtime.block_on(async {
            self.node
                .connected_peers()
                .await
                .into_iter()
                .map(|(peer, alpns)| (peer.to_string(), alpns.into_iter().collect()))
                .collect()
        })
    }
}
