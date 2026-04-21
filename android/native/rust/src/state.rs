use acerola_p2p::api::AcerolaP2P;
use jni::objects::GlobalRef;
use once_cell::sync::OnceCell;
use std::fmt;
use std::sync::Arc;
use tokio::runtime::Runtime;

pub struct AppState {
    pub node: Arc<AcerolaP2P>,
    pub runtime: Runtime,
    pub callback: GlobalRef,
    pub jvm: jni::JavaVM,
}

impl fmt::Debug for AppState {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("AppState")
            .field("node", &"Arc<AcerolaP2P>")
            .field("runtime", &"Runtime")
            .field("callback", &"GlobalRef")
            .field("jvm", &"JavaVM")
            .finish()
    }
}

pub static INSTANCE: OnceCell<AppState> = OnceCell::new();
