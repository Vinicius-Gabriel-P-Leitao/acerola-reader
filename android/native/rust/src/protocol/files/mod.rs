mod exchange;
mod model;

use std::sync::Arc;

use acerola_p2p::api::{
    error::P2pError,
    peer::PeerIdentity,
    protocol::{EventEmitter, Handler},
};
use async_trait::async_trait;
use tokio::io::{AsyncRead, AsyncWrite};

use crate::callbacks::FileSyncProvider;
use model::FileSyncStats;

pub(crate) const FILE_SYNC_ALPN: &[u8] = b"acerola/sync-files/1";

/// Papel outbound do protocolo `acerola/sync-files/1` — este lado iniciou a conexão.
pub(crate) struct FileSyncOutbound {
    emit: EventEmitter,
    provider: Arc<dyn FileSyncProvider>,
}

impl FileSyncOutbound {
    pub(crate) fn new(emit: EventEmitter, provider: Arc<dyn FileSyncProvider>) -> Self {
        Self { emit, provider }
    }
}

#[async_trait]
impl Handler for FileSyncOutbound {
    async fn handle(
        &self, peer: &PeerIdentity, send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), P2pError> {
        run_and_report(true, peer, &self.emit, &self.provider, send, recv).await
    }
}

/// Papel inbound do protocolo `acerola/sync-files/1` — o peer iniciou a conexão.
pub(crate) struct FileSyncInbound {
    emit: EventEmitter,
    provider: Arc<dyn FileSyncProvider>,
}

impl FileSyncInbound {
    pub(crate) fn new(emit: EventEmitter, provider: Arc<dyn FileSyncProvider>) -> Self {
        Self { emit, provider }
    }
}

#[async_trait]
impl Handler for FileSyncInbound {
    async fn handle(
        &self, peer: &PeerIdentity, send: Box<dyn AsyncWrite + Send + Unpin>,
        recv: Box<dyn AsyncRead + Send + Unpin>,
    ) -> Result<(), P2pError> {
        run_and_report(false, peer, &self.emit, &self.provider, send, recv).await
    }
}

async fn run_and_report(
    outbound_role: bool, peer: &PeerIdentity, emit: &EventEmitter, provider: &Arc<dyn FileSyncProvider>,
    send: Box<dyn AsyncWrite + Send + Unpin>, recv: Box<dyn AsyncRead + Send + Unpin>,
) -> Result<(), P2pError> {
    match exchange::run_exchange(outbound_role, peer, emit, provider, send, recv).await {
        Ok(stats) => {
            emit("sync:files:complete", complete_payload(peer, &stats));
            Ok(())
        },
        Err(err) => {
            emit(
                "sync:files:chapter_failed",
                serde_json::json!({ "peerId": peer.id, "comicName": "", "chapter": "", "reason": err.to_string() })
                    .to_string(),
            );
            Err(err)
        },
    }
}

fn complete_payload(peer: &PeerIdentity, stats: &FileSyncStats) -> String {
    serde_json::json!({
        "peerId": peer.id,
        "receivedCount": stats.received_count,
        "sentCount": stats.sent_count,
        "failedCount": stats.failed_count,
    })
    .to_string()
}
