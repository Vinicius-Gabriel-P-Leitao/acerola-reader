use acerola_p2p::api::{guard::open_guard, AcerolaP2P};
use once_cell::sync::Lazy;
use std::sync::Arc;
use tokio::runtime::Runtime;

uniffi::setup_scaffolding!();

#[uniffi::export]
pub trait P2PCallback: Send + Sync {
    fn on_event(&self, event: String, data: String);
}

static TOKIO_RUNTIME: Lazy<Arc<Runtime>> = Lazy::new(|| Arc::new(Runtime::new().expect("Failed to create Tokio runtime")));

#[derive(uniffi::Object)]
pub struct P2PNode {
    node: Arc<AcerolaP2P>,
    runtime: Arc<Runtime>,
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
}
