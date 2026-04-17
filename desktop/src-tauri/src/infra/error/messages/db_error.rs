use thiserror::Error;

#[derive(Debug, Error)]
pub enum DbError {
    #[error("Record not found.")]
    NotFound,

    #[error("Unique constraint violation.")]
    UniqueViolation,

    #[error("Foreign key constraint violation.")]
    ForeignKeyViolation,

    #[error("Check constraint violation.")]
    CheckViolation,

    #[error("Internal database error: {0}")]
    Internal(sqlx::Error),
}
