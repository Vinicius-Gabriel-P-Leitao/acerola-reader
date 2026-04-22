pub mod guard {
    pub use crate::guard::token_guard;
}
pub mod mode {
    pub use crate::mode::FfiNetworkMode;
}
pub mod singleton {
    pub use crate::singleton::TOKIO_RUNTIME;
}

use acerola_p2p::api::{
    guard::{open_guard, Guard},
    network::NetworkMode,
    AcerolaP2P,
};

use crate::{guard::token_guard, mode::FfiNetworkMode, singleton::TOKIO_RUNTIME};
use std::{collections::HashMap, sync::Arc};
use tokio::{runtime::Runtime, sync::RwLock};

#[uniffi::export(with_foreign)]
pub trait P2PCallback: Send + Sync {
    fn on_event(&self, event: String, data: String);
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
            let cb_clone = Arc::clone(&callback);

            let emit: acerola_p2p::api::protocol::EventEmitter = Arc::new(move |event, data| {
                cb_clone.on_event(event.to_string(), data);
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
