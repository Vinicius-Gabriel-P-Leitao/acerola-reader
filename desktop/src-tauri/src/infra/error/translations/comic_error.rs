use crate::infra::error::translations::db_error::DbError;
use thiserror::Error;

#[derive(Debug, Error)]
pub enum ComicError {
    #[error("Este quadrinho já existe na sua biblioteca.")]
    AlreadyExists,

    #[error("Falha no sistema ao tentar salvar o quadrinho.")]
    SystemFailure(String),

    #[error("Erro de acesso a arquivos: {0}")]
    Io(std::io::Error),

    #[error("Erro de banco: {0}")]
    Database(DbError),
}

impl From<DbError> for ComicError {
    fn from(db_err: DbError) -> Self {
        match db_err {
            // Aqui damos significado de negócio a um erro de banco!
            DbError::UniqueViolation => ComicError::AlreadyExists,

            // Qualquer outro erro de banco vira uma falha genérica de sistema
            DbError::NotFound
            | DbError::ForeignKeyViolation
            | DbError::CheckViolation
            | DbError::Internal(_) => ComicError::SystemFailure(db_err.to_string()),
        }
    }
}

impl From<std::io::Error> for ComicError {
    fn from(io_err: std::io::Error) -> Self {
        ComicError::Io(io_err)
    }
}
