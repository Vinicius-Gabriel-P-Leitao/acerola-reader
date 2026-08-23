//! Remoção "lógica" de um blob por hash — `iroh-blobs` só expõe deleção física via GC.
//!
//! `iroh_blobs::api::blobs::Blobs::delete` (e a função de varredura `gc_run_once`) são privados
//! de propósito no crate ("users should rely only on garbage collection for blob deletion"), e
//! nem sequer são alcançáveis de fora do crate. A única forma pública de influenciar isso é
//! configurar um `GcConfig` na criação do `Store` (ver `config.rs`) — o próprio store então roda
//! sua varredura periódica internamente. `remove()` aqui faz a parte que É pública e imediata:
//! apaga a(s) tag(s) que protegem o hash, deixando-o elegível pro próximo ciclo de GC do store.

use futures::StreamExt;
use iroh_blobs::{api::Store, Hash};

use crate::infra::error::ConnectionError;

pub(super) async fn untag(store: &Store, hash: Hash) -> Result<(), ConnectionError> {
    for name in tags_for_hash(store, hash).await? {
        store.tags().delete(name).await?;
    }
    Ok(())
}

async fn tags_for_hash(store: &Store, hash: Hash) -> Result<Vec<Vec<u8>>, ConnectionError> {
    let mut tags = store.tags().list().await?;
    let mut matching = Vec::new();

    while let Some(tag) = tags.next().await {
        let info = tag?;
        if info.hash_and_format().hash == hash {
            matching.push(info.name.0.to_vec());
        }
    }

    Ok(matching)
}
