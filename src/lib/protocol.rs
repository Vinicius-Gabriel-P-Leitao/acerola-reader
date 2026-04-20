#[path = "protocol/rpc.rs"]
pub(crate) mod rpc;

use std::sync::Arc;

use async_trait::async_trait;
use tokio::io::{AsyncRead, AsyncWrite};

use crate::error::ConnectionError;
use crate::peer::PeerId;

pub type EventEmitter = Arc<dyn Fn(&str, String) + Send + Sync>;

#[async_trait]
pub trait ProtocolHandler: Send + Sync {
    async fn handle(
        &self, peer: &PeerId, send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), ConnectionError>;
}
