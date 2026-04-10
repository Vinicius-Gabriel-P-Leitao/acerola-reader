use thiserror::Error;

/// Representa os erros semânticos que podem ocorrer ao interagir com o banco de dados.
///
/// Esta é a única camada da aplicação que conhece os códigos internos do SQLite.
/// Todos os erros do [`sqlx`] devem ser convertidos para `DbError` antes de subir
/// para as camadas superiores — services e commands nunca devem ver um [`sqlx::Error`] diretamente.
///
/// A conversão acontece via [`From<sqlx::Error>`] implementado abaixo, e é acionada
/// automaticamente pelo operador `?` nos repositórios.
#[derive(Debug, Error)]
pub enum DbError {
    /// Nenhum registro foi encontrado para a query executada.
    ///
    /// Mapeado a partir de [`sqlx::Error::RowNotFound`], que é retornado pelo sqlx
    /// quando `fetch_one` não encontra nenhuma linha. Operações com `fetch_all` e
    /// `fetch_optional` **não** disparam este erro — retornam vazio ou `None`.
    #[error("Registro não encontrado.")]
    NotFound,

    /// Violação de restrição de unicidade (`UNIQUE` ou `PRIMARY KEY`).
    ///
    /// Ocorre quando uma inserção ou atualização tenta criar um valor duplicado
    /// em uma coluna com restrição `UNIQUE`. No SQLite, cobre tanto `UNIQUE`
    /// quanto `PRIMARY KEY`, pois ambos compartilham o mesmo código de erro interno.
    #[error("Violação de restrição de unicidade (Unique Constraint)")]
    UniqueViolation,

    /// Violação de chave estrangeira (`FOREIGN KEY`).
    ///
    /// Ocorre quando uma inserção ou atualização referencia um id inexistente
    /// em outra tabela. No SQLite, **requer** `PRAGMA foreign_keys = ON` para ser
    /// ativado — sem ele, a restrição é ignorada silenciosamente.
    #[error("Violação de chave estrangeira (Foreign Key Constraint)")]
    ForeignKeyViolation,

    /// Violação de restrição de verificação (`CHECK`).
    ///
    /// Ocorre quando um valor inserido ou atualizado não satisfaz a condição
    /// definida por uma cláusula `CHECK` na definição da tabela.
    #[error("Violação de restrição de verificação (Check Constraint)")]
    CheckViolation,

    /// Erro interno do banco de dados não mapeado explicitamente.
    ///
    /// Captura qualquer erro do [`sqlx`] que não se encaixe nas variantes acima.
    /// O erro original é preservado para fins de diagnóstico — use `.to_string()`
    /// ou `{:?}` para inspecioná-lo em logs.
    #[error("Erro interno do banco de dados: {0}")]
    Internal(sqlx::Error),
}

/// Converte [`sqlx::Error`] em [`DbError`], mapeando os erros conhecidos do SQLite
/// para variantes semânticas e delegando o restante para [`DbError::Internal`].
///
/// É o único lugar da aplicação que inspeciona códigos de erro do SQLite.
/// O operador `?` nos repositórios aciona esta conversão automaticamente.
impl From<sqlx::Error> for DbError {
    fn from(err: sqlx::Error) -> Self {
        if let sqlx::Error::RowNotFound = err {
            return DbError::NotFound;
        }

        if let sqlx::Error::Database(ref db) = err {
            if db.is_unique_violation() {
                return DbError::UniqueViolation;
            }
            if db.is_foreign_key_violation() {
                return DbError::ForeignKeyViolation;
            }
            if db.is_check_violation() {
                return DbError::CheckViolation;
            }
        }

        DbError::Internal(err)
    }
}
