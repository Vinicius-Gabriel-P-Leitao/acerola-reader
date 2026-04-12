use crate::infra::error::translations::db_error::DbError;
use crate::infra::error::translations::path_error::PathError;
use thiserror::Error;

/// Representa os erros de negócio relacionados a quadrinhos.
///
/// Esta camada dá **significado de domínio** aos erros técnicos vindos das
/// camadas inferiores (banco de dados, sistema de arquivos). Services e commands
/// nunca devem ver um [`sqlx::Error`] ou [`std::io::Error`] diretamente — apenas
/// variantes desta enum.
#[derive(Debug, Error)]
pub enum ComicError {
    /// O quadrinho já existe na biblioteca.
    ///
    /// Mapeado a partir de [`DbError::UniqueViolation`] — indica que uma inserção
    /// violou a restrição de unicidade da tabela `comic_directory`.
    #[error("Comic already exists in the library.")]
    AlreadyExists,

    /// O quadrinho solicitado não foi encontrado.
    ///
    /// Mapeado a partir de [`DbError::NotFound`] — ocorre quando uma operação
    /// usa `fetch_one` e nenhuma linha corresponde ao critério da query.
    #[error("Comic not found.")]
    NotFound,

    /// Os dados enviados violam uma regra de validação do banco.
    ///
    /// Mapeado a partir de [`DbError::CheckViolation`] — indica que um valor
    /// inserido ou atualizado não satisfaz uma restrição `CHECK` da tabela.
    #[error("Invalid data: {0}")]
    InvalidRequest(String),

    /// Referência a um registro relacionado que não existe.
    ///
    /// Mapeado a partir de [`DbError::ForeignKeyViolation`] — indica uma violação
    /// de integridade referencial. Geralmente significa que o dado pai foi removido
    /// ou nunca existiu, o que aponta para um problema de sincronização ou lógica.
    #[error("Invalid or missing reference to a related record.")]
    IntegrityViolation,

    /// Falha genérica de sistema sem representação de negócio definida.
    ///
    /// Carrega uma mensagem descritiva do erro original. O erro técnico completo
    /// é logado antes desta variante ser construída, preservando o diagnóstico
    /// nos logs mesmo que o frontend receba apenas a mensagem genérica.
    #[error("System failure while processing the comic.")]
    SystemFailure(String),

    /// Erro de acesso ao sistema de arquivos.
    ///
    /// Propagado diretamente de [`std::io::Error`] via [`From`].
    #[error("Filesystem access error: {0}")]
    Io(std::io::Error),
}


impl From<DbError> for ComicError {
    fn from(db_err: DbError) -> Self {
        match db_err {
            DbError::UniqueViolation => {
                log::debug!("[ComicError] Duplicate comic blocked by UNIQUE constraint.");
                ComicError::AlreadyExists
            }

            DbError::NotFound => {
                log::warn!(
                    "[ComicError] Record not found — possible race condition or invalid id."
                );
                ComicError::NotFound
            }

            DbError::CheckViolation => {
                log::warn!("[ComicError] Invalid data rejected by CHECK constraint.");
                ComicError::InvalidRequest(DbError::CheckViolation.to_string())
            }

            DbError::ForeignKeyViolation => {
                log::error!("[ComicError] Referential integrity violation — parent record missing or removed.");
                ComicError::IntegrityViolation
            }

            DbError::Internal(ref err) => {
                log::error!("[ComicError] Unmapped internal database error: {:?}", err);
                ComicError::SystemFailure(db_err.to_string())
            }
        }
    }
}

impl From<std::io::Error> for ComicError {
    fn from(io_err: std::io::Error) -> Self {
        ComicError::Io(io_err)
    }
}

impl From<PathError> for ComicError {
    fn from(err: PathError) -> Self {
        match err {
            PathError::AccessDenied => {
                log::warn!("[ComicError] Access denied by PathGuard — path outside allowed root.");
                ComicError::InvalidRequest(err.to_string())
            }

            PathError::NotFound(_) => {
                log::warn!("[ComicError] Path not found: {}", err);
                ComicError::NotFound
            }

            PathError::ActionFailed(_) => {
                log::error!("[ComicError] Filesystem operation failed: {}", err);
                ComicError::SystemFailure(err.to_string())
            }
        }
    }
}
