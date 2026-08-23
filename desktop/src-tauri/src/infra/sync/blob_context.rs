//! Ponte entre os protocolos de aplicação (`sync-files`, `sync-comic`, futuramente
//! `browse-cover`) e a capacidade de blobs do node. O `Handler` da lib (`acerola-p2p`) só
//! entrega a `PeerIdentity` de quem chamou, sem endereço de rede — mas `P2pBlobStore::fetch`
//! precisa de um `PeerAddr` completo (abre uma conexão QUIC própria, independente do stream do
//! protocolo em si) e do próprio `Arc<dyn P2pBlobStore>`, os dois só disponíveis via métodos do
//! `AcerolaP2p` (`node.blobs()`, `node.known_peers()`).
//!
//! O node só existe DEPOIS que `AcerolaP2p::builder(...).build()` retorna, mas os handlers são
//! passados PRO builder antes disso — não dá pra injetar `Arc<AcerolaP2p>` direto na construção
//! deles. `BlobContext` resolve isso guardando um `Weak<AcerolaP2p>` preenchido uma única vez
//! (`set_node`), logo depois do `.build()` em `bios/network.rs`, antes de qualquer conexão poder
//! disparar um handler (nenhuma existe até o node ser devolvido).

use std::sync::{Arc, OnceLock, Weak};

use acerola_p2p::api::{
    blobs::P2pBlobStore,
    error::P2pError,
    peer::{PeerAddr, PeerIdentity},
    AcerolaP2p,
};

#[derive(Default)]
pub struct BlobContext {
    node: OnceLock<Weak<AcerolaP2p>>,
}

impl BlobContext {
    pub fn new() -> Arc<Self> {
        Arc::new(Self::default())
    }

    pub fn set_node(&self, node: &Arc<AcerolaP2p>) {
        let _ = self.node.set(Arc::downgrade(node));
    }

    fn node(&self) -> Result<Arc<AcerolaP2p>, P2pError> {
        self.node
            .get()
            .and_then(Weak::upgrade)
            .ok_or_else(|| P2pError::StreamFailed("p2p node not initialized yet".into()))
    }

    pub async fn blob_store(&self) -> Result<Arc<dyn P2pBlobStore>, P2pError> {
        self.node()?
            .blobs()
            .await
            .ok_or_else(|| P2pError::StreamFailed("blob store not configured on this node".into()))
    }

    /// Resolve o `PeerAddr` mais recente conhecido para `peer` — cache alimentado pelo
    /// handshake (`acerola/handshake/1`) e por chamadas explícitas (`connect`/`sync_comic`).
    pub async fn resolve_addr(&self, peer: &PeerIdentity) -> Result<PeerAddr, P2pError> {
        self.node()?
            .known_peers()
            .await
            .into_iter()
            .find(|(id, _, _)| id == peer)
            .map(|(_, addr, _)| addr)
            .ok_or_else(|| P2pError::StreamFailed(format!("no known address for peer {}", peer.id)))
    }
}
