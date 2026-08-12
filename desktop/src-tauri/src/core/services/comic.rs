use std::path::PathBuf;

use sqlx::SqlitePool;

use crate::{
    core::services::archive::comic_scanner_engine::ComicScannerService,
    data::{
        models::archive::comic_directory::ComicDirectory,
        repositories::archive::comic_directory_repo::ComicRepository,
    },
    infra::error::ComicError,
};

/// Serviço para operações em massa e individuais de quadrinhos.
pub struct ComicService {
    repo: ComicRepository,
    pool: SqlitePool,
}

impl ComicService {
    pub fn new(pool: SqlitePool) -> Self {
        Self { repo: ComicRepository::new(pool.clone()), pool }
    }

    /// Reescaneia pontualmente um único quadrinho a partir do seu path atual no disco.
    pub async fn rescan_comic(&self, id: i64) -> Result<(), ComicError> {
        let comic = self.repo.find_by_id(id).await.map_err(ComicError::from)?.ok_or(ComicError::NotFound)?;
        let scanner = ComicScannerService::new(PathBuf::from(&comic.path), self.pool.clone());

        scanner.rescan_comic(comic, |_| {}, |_| {}).await
    }

    /// Invalida e reescaneia um único quadrinho do zero (capítulos e volumes inclusos).
    pub async fn deep_rescan_comic(&self, id: i64) -> Result<(), ComicError> {
        let comic = self.repo.find_by_id(id).await.map_err(ComicError::from)?.ok_or(ComicError::NotFound)?;
        let scanner = ComicScannerService::new(PathBuf::from(&comic.path), self.pool.clone());

        scanner.deep_rescan_comic(comic, |_| {}, |_| {}).await
    }

    /// Atualiza o status de visibilidade de um quadrinho específico.
    pub async fn update_hidden_status(
        &self, id: i64, hidden: bool,
    ) -> Result<ComicDirectory, ComicError> {
        self.repo.update_hidden_status(id, hidden).await.map_err(ComicError::from)
    }

    /// Atualiza o status de sincronizacao externa de um quadrinho específico.
    pub async fn update_external_sync_enabled(
        &self, id: i64, external_sync_enabled: bool,
    ) -> Result<ComicDirectory, ComicError> {
        self.repo
            .update_external_sync_enabled(id, external_sync_enabled)
            .await
            .map_err(ComicError::from)
    }

    /// Atualiza o status de visibilidade de multiplos quadrinhos em batch.
    pub async fn update_hidden_status_batch(
        &self, ids: &[i64], hidden: bool,
    ) -> Result<usize, ComicError> {
        self.repo.update_hidden_status_batch(ids, hidden).await.map_err(ComicError::from)
    }

    /// Deleta multiplos quadrinhos do banco de dados.
    pub async fn delete_batch(&self, ids: &[i64]) -> Result<usize, ComicError> {
        self.repo.delete_batch(ids).await.map_err(ComicError::from)
    }
}
