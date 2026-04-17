use thiserror::Error;

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
