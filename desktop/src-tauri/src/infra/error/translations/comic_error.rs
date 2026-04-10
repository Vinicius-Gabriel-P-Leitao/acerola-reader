use crate::infra::error::translations::db_error::DbError;
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
    #[error("Este quadrinho já existe na sua biblioteca.")]
    AlreadyExists,

    /// O quadrinho solicitado não foi encontrado.
    ///
    /// Mapeado a partir de [`DbError::NotFound`] — ocorre quando uma operação
    /// usa `fetch_one` e nenhuma linha corresponde ao critério da query.
    #[error("Quadrinho não encontrado.")]
    NotFound,

    /// Os dados enviados violam uma regra de validação do banco.
    ///
    /// Mapeado a partir de [`DbError::CheckViolation`] — indica que um valor
    /// inserido ou atualizado não satisfaz uma restrição `CHECK` da tabela.
    /// Geralmente representa um dado inválido enviado pela camada superior.
    #[error("Dado inválido: {0}")]
    InvalidRequest(String),

    /// Referência a um registro relacionado que não existe.
    ///
    /// Mapeado a partir de [`DbError::ForeignKeyViolation`] — indica uma violação
    /// de integridade referencial. Geralmente significa que o dado pai foi removido
    /// ou nunca existiu, o que aponta para um problema de sincronização ou lógica.
    #[error("Referência a um registro relacionado inválida ou inexistente.")]
    IntegrityViolation,

    /// Falha genérica de sistema sem representação de negócio definida.
    ///
    /// Carrega uma mensagem descritiva do erro original. O erro técnico completo
    /// é logado antes desta variante ser construída, preservando o diagnóstico
    /// nos logs mesmo que o frontend receba apenas a mensagem genérica.
    #[error("Falha no sistema ao tentar salvar o quadrinho.")]
    SystemFailure(String),

    /// Erro de acesso ao sistema de arquivos.
    ///
    /// Propagado diretamente de [`std::io::Error`] via [`From`].
    #[error("Erro de acesso a arquivos: {0}")]
    Io(std::io::Error),
}

// prettier-ignore
impl From<DbError> for ComicError {
    fn from(db_err: DbError) -> Self {
        match db_err {
            DbError::UniqueViolation => {
                log::debug!("[ComicError] Quadrinho duplicado bloqueado pela constraint UNIQUE.");
                ComicError::AlreadyExists
            }
            
            DbError::NotFound => {
                log::warn!("[ComicError] Registro não encontrado — possível race condition ou id inválido.");
                ComicError::NotFound
            }
            
            DbError::CheckViolation => {
                log::warn!("[ComicError] Dado inválido rejeitado pela constraint CHECK.");
                ComicError::InvalidRequest(DbError::CheckViolation.to_string())
            }

            DbError::ForeignKeyViolation => {
                log::error!("[ComicError] Violação de integridade referencial — registro pai ausente ou removido.");
                ComicError::IntegrityViolation
            }
            
            DbError::Internal(ref err) => {
                log::error!("[ComicError] Erro interno de banco não mapeado: {:?}", err);
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
