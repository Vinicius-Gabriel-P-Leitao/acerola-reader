use thiserror::Error;

#[derive(Debug, Error)]
pub enum ComicError {
    #[error("Comic already exists in the library.")]
    AlreadyExists,

    #[error("Comic not found.")]
    NotFound,

    #[error("Invalid data: {0}")]
    InvalidRequest(String),

    #[error("Invalid or missing reference to a related record.")]
    IntegrityViolation,

    #[error("System failure while processing the comic.")]
    SystemFailure(String),

    #[error("Filesystem access error: {0}")]
    Io(std::io::Error),
}
