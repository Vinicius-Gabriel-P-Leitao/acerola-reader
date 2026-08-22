//! Ponte entre `IrohTransport` e o adapter opcional `IrohBlobStore`.
//!
//! `iroh` (transporte) e `iroh-blobs-adapter` (blobs) são features Cargo independentes — dá pra
//! ligar `iroh` sozinho. `BlobsIntegration` existe pra `transport.rs`/`builder.rs` não precisarem
//! de `#[cfg(feature = "iroh-blobs-adapter")]` espalhado: com a feature desligada, vira um
//! "null object" que nunca tem blob store configurado; com ela ligada, monta de fato o
//! `IrohBlobStore` e o `BlobsProtocol` real do iroh-blobs sobre o `Endpoint` do transporte.

#[cfg(feature = "iroh-blobs-adapter")]
mod enabled {
    use std::sync::Arc;

    use iroh::{endpoint::Connection, protocol::ProtocolHandler as _, Endpoint};
    use iroh_blobs::BlobsProtocol;

    use crate::{
        core::blobs::{
            iroh::{IrohBlobStore, IrohBlobsConfig},
            P2pBlobStore,
        },
        infra::error::ConnectionError,
    };

    /// Configuração de blobs opcional repassada pelo app consumidor via `IrohTransportBuilder`.
    pub type BlobsConfigSlot = Option<IrohBlobsConfig>;

    /// ALPN a incluir na lista do `Endpoint` antes de bindar, se blobs estiver configurado.
    ///
    /// Não depende do `Endpoint` (diferente de `BlobsIntegration::configure`) porque os ALPNs
    /// precisam ser conhecidos *antes* do bind — a ordem de construção é: decidir ALPNs → bindar
    /// → só então montar o `IrohBlobStore` de fato sobre o `Endpoint` já vivo.
    pub fn wants_alpn(slot: &BlobsConfigSlot) -> Option<&'static [u8]> {
        slot.as_ref().map(|_| iroh_blobs::ALPN)
    }

    #[derive(Clone, Default)]
    pub struct BlobsIntegration(Option<Arc<IrohBlobStore>>);

    impl BlobsIntegration {
        pub async fn configure(
            slot: &BlobsConfigSlot, endpoint: &Endpoint,
        ) -> Result<Self, ConnectionError> {
            let Some(config) = slot else { return Ok(Self(None)) };
            let store = IrohBlobStore::new(config, endpoint.clone()).await?;
            Ok(Self(Some(Arc::new(store))))
        }

        /// Capacidade exposta via `P2pTransport::blobs()`.
        pub fn as_capability(&self) -> Option<Arc<dyn P2pBlobStore>> {
            self.0.clone().map(|store| store as Arc<dyn P2pBlobStore>)
        }

        /// Se a conexão for do ALPN de blobs, repassa pro `BlobsProtocol` real (numa task de
        /// fundo, dona de uma cópia da `Connection`) e retorna `true`. Do contrário retorna
        /// `false` sem tocar em `conn` — quem chamou continua livre pra usá-la normalmente.
        pub async fn try_accept(&self, alpn: &[u8], conn: &Connection) -> bool {
            let Some(store) = &self.0 else { return false };
            if alpn != iroh_blobs::ALPN {
                return false;
            }

            let protocol = BlobsProtocol::new(store.inner_store(), None);
            let conn = conn.clone();
            tokio::spawn(async move {
                if let Err(err) = protocol.accept(conn).await {
                    tracing::debug!(layer = "iroh_blobs", error = ?err, "blob transfer session failed");
                }
            });
            true
        }
    }
}

#[cfg(not(feature = "iroh-blobs-adapter"))]
mod disabled {
    use std::sync::Arc;

    use iroh::{endpoint::Connection, Endpoint};

    use crate::{core::blobs::P2pBlobStore, infra::error::ConnectionError};

    /// Sem a feature `iroh-blobs-adapter`, não há nenhuma configuração de blobs possível.
    pub type BlobsConfigSlot = ();

    pub fn wants_alpn(_slot: &BlobsConfigSlot) -> Option<&'static [u8]> {
        None
    }

    #[derive(Clone, Default)]
    pub struct BlobsIntegration;

    impl BlobsIntegration {
        pub async fn configure(
            _slot: &BlobsConfigSlot, _endpoint: &Endpoint,
        ) -> Result<Self, ConnectionError> {
            Ok(Self)
        }

        pub fn as_capability(&self) -> Option<Arc<dyn P2pBlobStore>> {
            None
        }

        pub async fn try_accept(&self, _alpn: &[u8], _conn: &Connection) -> bool {
            false
        }
    }
}

#[cfg(not(feature = "iroh-blobs-adapter"))]
pub use disabled::{wants_alpn, BlobsConfigSlot, BlobsIntegration};
#[cfg(feature = "iroh-blobs-adapter")]
pub use enabled::{wants_alpn, BlobsConfigSlot, BlobsIntegration};
