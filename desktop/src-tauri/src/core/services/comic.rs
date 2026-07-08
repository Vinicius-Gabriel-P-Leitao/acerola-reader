use sqlx::SqlitePool;

use crate::{
    data::{
        models::archive::comic_directory::ComicDirectory,
        repositories::archive::comic_directory_repo::ComicRepository,
    },
    infra::error::ComicError,
};

/// Serviço para operações em massa e individuais de quadrinhos.
pub struct ComicService {
    repo: ComicRepository,
}

impl ComicService {
    pub fn new(pool: SqlitePool) -> Self {
        Self { repo: ComicRepository::new(pool) }
    }

    /// Atualiza o status de visibilidade de um quadrinho específico.
    pub async fn update_hidden_status(
        &self, id: i64, hidden: bool,
    ) -> Result<ComicDirectory, ComicError> {
        self.repo.update_hidden_status(id, hidden).await.map_err(ComicError::from)
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
