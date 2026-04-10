use thiserror::Error;

/// Erros que podem ocorrer durante a validação de arquivos pelos guards.
#[derive(Debug, Error)]
pub enum FileError {
    /// A extensão do arquivo não é permitida por este guard.
    #[error("Extension not allowed: .{0}")]
    ExtensionNotAllowed(String),

    /// O nome do arquivo não é permitido por este guard.
    #[error("File not allowed: {0}")]
    FileNameNotAllowed(String),

    /// O arquivo não possui extensão.
    #[error("File has no extension.")]
    MissingExtension,

    /// O caminho não contém nome de arquivo.
    #[error("Path has no file name.")]
    MissingFileName,

    /// Nenhum guard aceitou o arquivo — será ignorado pelo scanner.
    ///
    /// Ocorre no [`ScannerGuard`] quando todos os guards individuais rejeitam
    /// o arquivo. É um caso esperado durante o scan — não indica falha.
    #[error("File not recognized by any guard: {0}")]
    NotAllowed(String),
}

impl FileError {
    pub fn not_allowed(name: &str) -> Self {
        log::debug!("[FileError] File skipped by scanner: {}", name);
        FileError::NotAllowed(name.to_string())
    }
}
