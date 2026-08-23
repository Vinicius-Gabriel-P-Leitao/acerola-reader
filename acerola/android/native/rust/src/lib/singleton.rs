use once_cell::sync::Lazy;
use std::sync::Arc;
use tokio::runtime::Runtime;

pub static TOKIO_RUNTIME: Lazy<Arc<Runtime>> =
    Lazy::new(|| Arc::new(Runtime::new().expect("Failed to create Tokio runtime")));
