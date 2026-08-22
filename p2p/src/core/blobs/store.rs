//! Contrato de armazenamento e transferência de blobs content-addressed.

use async_trait::async_trait;
use tokio::io::AsyncRead;

use super::hash::BlobHash;
use crate::infra::{error::ConnectionError, peer::PeerAddr};

/// Contrato para armazenamento local e transferência de blobs content-addressed.
///
/// Mesmo espírito de `P2pTransport`: qualquer adapter (`InMemoryBlobStore` para testes,
/// `IrohBlobStore` atrás da feature `iroh-blobs-adapter`) pode implementar esta trait sem vazar
/// detalhe de implementação para quem consome a API pública.
#[async_trait]
pub trait P2pBlobStore: Send + Sync {
    /// Armazena os bytes fornecidos e retorna seu identificador content-addressed.
    async fn put(&self, data: Vec<u8>) -> Result<BlobHash, ConnectionError>;

    /// Abre um stream de leitura para o blob local associado ao hash.
    ///
    /// Retorna `ConnectionError::BlobNotFound` se o hash não estiver no store local — use
    /// `fetch` primeiro para baixar de um peer remoto quando necessário.
    async fn get(
        &self, hash: &BlobHash,
    ) -> Result<Box<dyn AsyncRead + Send + Unpin>, ConnectionError>;

    /// Verifica se o blob já está disponível localmente, sem baixar o conteúdo.
    async fn has(&self, hash: &BlobHash) -> Result<bool, ConnectionError>;

    /// Remove o blob do store local, se presente.
    async fn remove(&self, hash: &BlobHash) -> Result<(), ConnectionError>;

    /// Baixa o blob de um peer remoto específico para o store local.
    ///
    /// Após retornar `Ok`, `get`/`has` para este hash passam a refletir o conteúdo baixado.
    async fn fetch(&self, hash: &BlobHash, from: &PeerAddr) -> Result<(), ConnectionError>;
}
