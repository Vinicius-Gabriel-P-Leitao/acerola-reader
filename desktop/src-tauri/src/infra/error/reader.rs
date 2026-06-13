use thiserror::Error;

#[derive(Debug, Error)]
pub enum ReaderError {
    #[error("Reader has no open chapter.")]
    ChapterNotOpen,

    #[error("Reader format is not supported: {0}")]
    UnsupportedFormat(String),

    #[error("Chapter has no readable image pages: {0}")]
    EmptyChapter(String),

    #[error("Page index out of bounds: {index}, total pages: {total}")]
    PageOutOfBounds { index: usize, total: usize },

    #[error("Page entry was listed but could not be read: index {index}, name {name}")]
    PageEntryMissing { index: usize, name: String },

    #[error("Filesystem error while reading chapter: {0}")]
    Io(std::io::Error),

    #[error("CBZ extraction failed: {0}")]
    Zip(zip::result::ZipError),

    #[error("CBR extraction failed: {0}")]
    Rar(unrar::error::UnrarError),

    #[error("Image type is not supported: {0}")]
    Image(String),

    #[error("Reader system failure: {0}")]
    SystemFailure(String),
}

impl From<std::io::Error> for ReaderError {
    fn from(err: std::io::Error) -> Self {
        match err.kind() {
            std::io::ErrorKind::NotFound => {
                log::warn!("[ReaderError] Chapter file not found: {}", err);
            },
            std::io::ErrorKind::PermissionDenied => {
                log::warn!("[ReaderError] Permission denied while reading chapter: {}", err);
            },
            _ => {
                log::error!("[ReaderError] Filesystem error while reading chapter: {}", err);
            },
        }

        ReaderError::Io(err)
    }
}

impl From<zip::result::ZipError> for ReaderError {
    fn from(err: zip::result::ZipError) -> Self {
        log::error!("[ReaderError] CBZ extraction failed: {}", err);
        ReaderError::Zip(err)
    }
}

impl From<unrar::error::UnrarError> for ReaderError {
    fn from(err: unrar::error::UnrarError) -> Self {
        log::error!("[ReaderError] CBR extraction failed: {}", err);
        ReaderError::Rar(err)
    }
}
