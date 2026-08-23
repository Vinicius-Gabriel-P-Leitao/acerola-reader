//! Configuração de armazenamento local para o adapter `IrohBlobStore`.

use std::{path::PathBuf, time::Duration};

use iroh_blobs::{
    api::Store,
    store::{fs::FsStore, mem::MemStore, GcConfig},
};

use crate::infra::error::ConnectionError;

/// Intervalo padrão de varredura de GC quando não especificado — curto o bastante para que
/// `P2pBlobStore::remove` reflita em `has()` num tempo previsível, sem sobrecarregar stores
/// pequenos e locais como os que esta lib gerencia.
const DEFAULT_GC_INTERVAL: Duration = Duration::from_secs(5);

/// Escolha do backend de armazenamento local usado pelo `IrohBlobStore`.
#[derive(Debug, Clone)]
pub enum IrohBlobsConfig {
    /// Store em memória — não sobrevive a reinícios, ideal para testes.
    Mem {
        /// Intervalo entre varreduras de garbage collection do store.
        gc_interval: Duration,
    },
    /// Store persistente em disco, na pasta informada.
    Fs {
        /// Diretório onde os blobs e metadados são persistidos.
        path: PathBuf,
        /// Intervalo entre varreduras de garbage collection do store.
        gc_interval: Duration,
    },
}

impl IrohBlobsConfig {
    /// Store em memória com o intervalo de GC padrão.
    pub fn mem() -> Self {
        Self::Mem { gc_interval: DEFAULT_GC_INTERVAL }
    }

    /// Store persistente em disco com o intervalo de GC padrão.
    pub fn fs(path: impl Into<PathBuf>) -> Self {
        Self::Fs { path: path.into(), gc_interval: DEFAULT_GC_INTERVAL }
    }

    /// Constrói (ou abre) o `Store` concreto do iroh-blobs a partir desta configuração.
    pub(super) async fn build_store(&self) -> Result<Store, ConnectionError> {
        match self {
            IrohBlobsConfig::Mem { gc_interval } => {
                let opts =
                    iroh_blobs::store::mem::Options { gc_config: Some(gc_config(*gc_interval)) };
                Ok(MemStore::new_with_opts(opts).into())
            },
            IrohBlobsConfig::Fs { path, gc_interval } => {
                let mut opts = iroh_blobs::store::fs::options::Options::new(path);
                opts.gc = Some(gc_config(*gc_interval));

                let store = FsStore::load_with_opts(path.clone(), opts).await?;
                Ok(store.into())
            },
        }
    }
}

fn gc_config(interval: Duration) -> GcConfig {
    GcConfig { interval, add_protected: None }
}
