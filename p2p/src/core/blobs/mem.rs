//! Implementação de referência em memória do `P2pBlobStore` (testes e consumidores sem adapter).

use std::{collections::HashMap, io::Cursor, sync::Arc};

use async_trait::async_trait;
use tokio::{io::AsyncRead, sync::RwLock};

use super::{hash::BlobHash, store::P2pBlobStore};
use crate::infra::{error::ConnectionError, peer::PeerAddr};

/// Store de blobs em memória, content-addressed via BLAKE3.
///
/// Sem capacidade de rede — `fetch` sempre retorna erro. Serve como store de referência para
/// testes e para consumidores que não ativaram nenhum adapter de blob com transporte real.
#[derive(Default, Clone)]
pub struct InMemoryBlobStore {
    blobs: Arc<RwLock<HashMap<BlobHash, Vec<u8>>>>,
}

impl InMemoryBlobStore {
    /// Instancia um novo store em memória vazio.
    pub fn new() -> Self {
        Self::default()
    }
}

#[async_trait]
impl P2pBlobStore for InMemoryBlobStore {
    async fn put(&self, data: Vec<u8>) -> Result<BlobHash, ConnectionError> {
        let hash = BlobHash::from_bytes(*blake3::hash(&data).as_bytes());
        self.blobs.write().await.insert(hash, data);
        Ok(hash)
    }

    async fn get(
        &self, hash: &BlobHash,
    ) -> Result<Box<dyn AsyncRead + Send + Unpin>, ConnectionError> {
        let data = self
            .blobs
            .read()
            .await
            .get(hash)
            .cloned()
            .ok_or_else(|| ConnectionError::BlobNotFound(hash.to_string()))?;
        Ok(Box::new(Cursor::new(data)))
    }

    async fn has(&self, hash: &BlobHash) -> Result<bool, ConnectionError> {
        Ok(self.blobs.read().await.contains_key(hash))
    }

    async fn remove(&self, hash: &BlobHash) -> Result<(), ConnectionError> {
        self.blobs.write().await.remove(hash);
        Ok(())
    }

    async fn fetch(&self, _hash: &BlobHash, _from: &PeerAddr) -> Result<(), ConnectionError> {
        Err(ConnectionError::StreamFailed(
            "fetch not supported by in-memory blob store".to_string(),
        ))
    }
}

#[cfg(test)]
mod tests {
    use tokio::io::AsyncReadExt;

    use super::*;
    use crate::infra::peer::PeerId;

    fn dummy_peer() -> PeerAddr {
        PeerAddr { id: PeerId { id: "peer-1".to_string(), device_id: None }, addrs: vec![] }
    }

    #[tokio::test]
    async fn put_then_get_returns_same_bytes() {
        let store = InMemoryBlobStore::new();
        let hash = store.put(b"hello world".to_vec()).await.unwrap();

        let mut reader = store.get(&hash).await.unwrap();
        let mut buf = Vec::new();
        reader.read_to_end(&mut buf).await.unwrap();

        assert_eq!(buf, b"hello world");
    }

    #[tokio::test]
    async fn has_reflects_presence_before_and_after_put() {
        let store = InMemoryBlobStore::new();
        let hash = BlobHash::from_bytes(*blake3::hash(b"data").as_bytes());

        assert!(!store.has(&hash).await.unwrap());
        store.put(b"data".to_vec()).await.unwrap();
        assert!(store.has(&hash).await.unwrap());
    }

    #[tokio::test]
    async fn remove_makes_has_return_false() {
        let store = InMemoryBlobStore::new();
        let hash = store.put(b"to be removed".to_vec()).await.unwrap();

        store.remove(&hash).await.unwrap();

        assert!(!store.has(&hash).await.unwrap());
    }

    #[tokio::test]
    async fn get_unknown_hash_returns_blob_not_found() {
        let store = InMemoryBlobStore::new();
        let unknown = BlobHash::from_bytes([0x99; 32]);

        let result = store.get(&unknown).await;

        assert!(matches!(result, Err(ConnectionError::BlobNotFound(_))));
    }

    #[tokio::test]
    async fn fetch_is_unsupported() {
        let store = InMemoryBlobStore::new();
        let hash = BlobHash::from_bytes([0x01; 32]);

        let result = store.fetch(&hash, &dummy_peer()).await;

        assert!(result.is_err());
    }

    #[tokio::test]
    async fn put_is_idempotent_for_identical_content() {
        let store = InMemoryBlobStore::new();
        let hash_a = store.put(b"same content".to_vec()).await.unwrap();
        let hash_b = store.put(b"same content".to_vec()).await.unwrap();

        assert_eq!(hash_a, hash_b);
    }
}
