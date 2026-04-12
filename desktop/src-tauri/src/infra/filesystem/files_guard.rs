use std::path::Path;

use crate::infra::{
    error::translations::file_error::FileError, pattern::archive_format::ArchiveFormat,
};

pub struct SupportedFileGuard;
pub struct ArchiveFileGuard;
pub struct MetadataFileGuard;
pub struct ArtworkFileGuard;

pub trait FileGuard: Send + Sync {
    fn is_allowed(&self, path: &Path) -> Result<(), FileError>;
}

impl FileGuard for SupportedFileGuard {
    fn is_allowed(&self, path: &Path) -> Result<(), FileError> {
        let ext = path
            .extension()
            .and_then(|ext| ext.to_str())
            .ok_or(FileError::MissingExtension)?;

        match ArchiveFormat::from_extension(ext) {
            Some(_) => Ok(()),
            None => Err(FileError::ExtensionNotAllowed(ext.to_string())),
        }
    }
}

impl FileGuard for ArchiveFileGuard {
    fn is_allowed(&self, path: &Path) -> Result<(), FileError> {
        let ext = path
            .extension()
            .and_then(|ext| ext.to_str())
            .ok_or(FileError::MissingExtension)?;

        match ArchiveFormat::from_extension(ext) {
            Some(ArchiveFormat::Pdf) | None => Err(FileError::ExtensionNotAllowed(ext.to_string())),
            Some(_) => Ok(()),
        }
    }
}

impl FileGuard for MetadataFileGuard {
    fn is_allowed(&self, path: &Path) -> Result<(), FileError> {
        let name = path
            .file_name()
            .and_then(|name| name.to_str())
            .ok_or(FileError::MissingFileName)?;

        match name {
            "ComicInfo.xml" => Ok(()),
            _ => Err(FileError::FileNameNotAllowed(name.to_string())),
        }
    }
}

impl FileGuard for ArtworkFileGuard {
    fn is_allowed(&self, path: &Path) -> Result<(), FileError> {
        let name = path
            .file_name()
            .and_then(|name| name.to_str())
            .ok_or(FileError::MissingFileName)?;

        match name {
            "cover.png" | "cover.jpg" | "cover.jpeg" | "banner.png" | "banner.jpg"
            | "banner.jpeg" => Ok(()),
            _ => Err(FileError::FileNameNotAllowed(name.to_string())),
        }
    }
}

pub struct ScannerGuard {
    guards: Vec<Box<dyn FileGuard>>,
}

impl ScannerGuard {
    pub fn new() -> Self {
        Self {
            guards: vec![
                Box::new(SupportedFileGuard),
                Box::new(MetadataFileGuard),
                Box::new(ArtworkFileGuard),
            ],
        }
    }

    /// Retorna `Ok` se ao menos um guard aceitar o arquivo.
    ///
    /// O scanner usa lógica de OR — um arquivo é válido se qualquer guard
    /// o reconhecer. Retorna [`FileError::NotAllowed`] se nenhum aceitar,
    /// o que é esperado para arquivos irrelevantes (`.db`, `.txt`, etc.).
    pub fn is_allowed(&self, path: &Path) -> Result<(), FileError> {
        let all_rejected = self
            .guards
            .iter()
            .all(|guard| guard.is_allowed(path).is_err());

        if all_rejected {
            let name = path
                .file_name()
                .and_then(|name| name.to_str())
                .unwrap_or("Unknown");

            return Err(FileError::not_allowed(name));
        }

        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::{ArtworkFileGuard, FileGuard, MetadataFileGuard, ScannerGuard, SupportedFileGuard};
    use crate::infra::error::translations::file_error::FileError;
    use std::path::Path;

    // NOTE: ComicFileGuard
    #[test]
    fn teste_comic_extensao_valida() {
        let guard = SupportedFileGuard;
        assert!(guard.is_allowed(Path::new("berserk.cbz")).is_ok());
        assert!(guard.is_allowed(Path::new("berserk.cbr")).is_ok());
        assert!(guard.is_allowed(Path::new("berserk.pdf")).is_ok());
    }

    #[test]
    fn teste_comic_extensao_invalida() {
        let guard = SupportedFileGuard;
        let result = guard.is_allowed(Path::new("berserk.exe"));

        assert!(matches!(result, Err(FileError::ExtensionNotAllowed(ext)) if ext == "exe"));
    }

    #[test]
    fn teste_comic_sem_extensao() {
        let guard = SupportedFileGuard;
        assert!(matches!(
            guard.is_allowed(Path::new("berserk")),
            Err(FileError::MissingExtension)
        ));
    }

    // NOTE: MetadataFileGuard
    #[test]
    fn teste_metadata_nome_valido() {
        let guard = MetadataFileGuard;
        assert!(guard.is_allowed(Path::new("ComicInfo.xml")).is_ok());
    }

    #[test]
    fn teste_metadata_nome_invalido() {
        let guard = MetadataFileGuard;
        let result = guard.is_allowed(Path::new("info.xml"));

        assert!(matches!(result, Err(FileError::FileNameNotAllowed(name)) if name == "info.xml"));
    }

    // NOTE: ArtworkFileGuard
    #[test]
    fn teste_artwork_nomes_validos() {
        let guard = ArtworkFileGuard;
        assert!(guard.is_allowed(Path::new("cover.png")).is_ok());
        assert!(guard.is_allowed(Path::new("cover.jpg")).is_ok());
        assert!(guard.is_allowed(Path::new("cover.jpeg")).is_ok());
        assert!(guard.is_allowed(Path::new("banner.png")).is_ok());
        assert!(guard.is_allowed(Path::new("banner.jpg")).is_ok());
        assert!(guard.is_allowed(Path::new("banner.jpeg")).is_ok());
    }

    #[test]
    fn teste_artwork_nome_invalido() {
        let guard = ArtworkFileGuard;
        let result = guard.is_allowed(Path::new("thumbnail.png"));

        assert!(
            matches!(result, Err(FileError::FileNameNotAllowed(name)) if name == "thumbnail.png")
        );
    }

    // NOTE: ScannerGuard
    #[test]
    fn teste_scanner_aceita_comic() {
        let guard = ScannerGuard::new();
        assert!(guard.is_allowed(Path::new("berserk.cbz")).is_ok());
    }

    #[test]
    fn teste_scanner_aceita_metadata() {
        let guard = ScannerGuard::new();
        assert!(guard.is_allowed(Path::new("ComicInfo.xml")).is_ok());
    }

    #[test]
    fn teste_scanner_aceita_artwork() {
        let guard = ScannerGuard::new();
        assert!(guard.is_allowed(Path::new("cover.png")).is_ok());
    }

    #[test]
    fn teste_scanner_rejeita_arquivo_desconhecido() {
        let guard = ScannerGuard::new();
        let result = guard.is_allowed(Path::new("script.sh"));

        assert!(matches!(result, Err(FileError::NotAllowed(_))));
    }
}
