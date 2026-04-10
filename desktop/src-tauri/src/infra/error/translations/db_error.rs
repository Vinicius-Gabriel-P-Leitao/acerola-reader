use thiserror::Error;

#[derive(Debug, Error)]
pub enum DbError {
    #[error("Violação de restrição de unicidade (Unique Constraint)")] UniqueViolation,

    #[error("Erro interno do banco de dados")] Internal(sqlx::Error),
}

// O único lugar da aplicação que sabe o que é o erro 1555
impl From<sqlx::Error> for DbError {
    fn from(err: sqlx::Error) -> Self {
        if let sqlx::Error::Database(ref db) = err {
            if db.is_unique_violation() {
                return DbError::UniqueViolation;
            }
        }

        DbError::Internal(err)
    }
}
