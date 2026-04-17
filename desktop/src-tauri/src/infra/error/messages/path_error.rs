use std::path::{Path, PathBuf};
use thiserror::Error;

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
        log::warn!(
            "[PathError] Access denied: {:?} is outside {:?}",
            canonical,
            root
        );
        PathError::AccessDenied
    }

    pub fn action_failed(path: &Path, msg: impl std::fmt::Display) -> Self {
        log::error!("[PathError] Operation failed on {:?}: {}", path, msg);
        PathError::ActionFailed(msg.to_string())
    }
}
