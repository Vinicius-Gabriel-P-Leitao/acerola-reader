use std::path::{Path, PathBuf};
use thiserror::Error;

pub mod comic;
pub mod db;
pub mod rpc;

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

#[derive(Debug, Error)]
pub enum FileError {
    #[error("Extension not allowed: .{0}")]
    ExtensionNotAllowed(String),

    #[error("File not allowed: {0}")]
    FileNameNotAllowed(String),

    #[error("File has no extension.")]
    MissingExtension,

    #[error("Path has no file name.")]
    MissingFileName,

    #[error("File not recognized by any guard: {0}")]
    NotAllowed(String),
}

impl FileError {
    pub fn not_allowed(name: &str) -> Self {
        log::debug!("[FileError] File skipped by scanner: {}", name);
        FileError::NotAllowed(name.to_string())
    }
}

#[derive(Debug, Error)]
pub enum PathError {
    #[error("Access denied: path is outside the allowed directory.")]
    AccessDenied,

    #[error("Path not found or inaccessible: {0}")]
    NotFound(PathBuf),

    #[error("Operation on path failed: {0}")]
    ActionFailed(String),
}

impl PathError {
    pub fn not_found(path: &Path) -> Self {
        log::debug!("[PathError] Path not found: {:?}", path);
        PathError::NotFound(path.to_path_buf())
    }

    pub fn access_denied(canonical: &Path, root: &Path) -> Self {
        log::warn!("[PathError] Access denied: {:?} is outside {:?}", canonical, root);
        PathError::AccessDenied
    }

    pub fn action_failed(path: &Path, msg: impl std::fmt::Display) -> Self {
        log::error!("[PathError] Operation failed on {:?}: {}", path, msg);
        PathError::ActionFailed(msg.to_string())
    }
}

// FIXME: Deve ser em ingles
#[derive(Debug, Error)]
pub enum PatternError {
    #[error("Macro mal formada: falta o fechamento '}}'.")]
    MalformedMacro,

    #[error("Macro desconhecida: '{0}'.")]
    UnknownMacro(String),

    #[error("O template deve conter exatamente um {{chapter}}.")]
    ChapterRequired,

    #[error("O template deve conter no máximo um {{decimal}}.")]
    DecimalDuplicate,

    #[error("O template deve conter exatamente um {{extension}}.")]
    ExtensionRequired,

    #[error("{{decimal}} deve vir depois de {{chapter}}.")]
    DecimalBeforeChapter,

    #[error("{{extension}} deve vir depois de {{chapter}}.")]
    ExtensionBeforeChapter,

    #[error("{{extension}} deve vir depois de {{decimal}}.")]
    ExtensionBeforeDecimal,

    #[error("O template deve terminar com {{extension}}.")]
    ExtensionNotAtEnd,

    #[error("Padrão de regex inválido gerado pelo template: {0}")]
    InvalidRegex(String),
}

#[derive(Debug, Error)]
pub enum RpcError {
    #[error("failed to serialize message: {0}")]
    Serialize(String),

    #[error("failed to deserialize message: {0}")]
    Deserialize(String),

    #[error("stream error: {0}")]
    Stream(String),
}
