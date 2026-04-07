use std::path::Path;

#[derive(Debug)]
pub enum FileGuardError {
    ExtensionNotAllowed(String),
    FileNameNotAllowed(String),
    MissingExtension,
    MissingFileName,
}

impl std::fmt::Display for FileGuardError {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        match self {
            Self::ExtensionNotAllowed(ext) => write!(f, "Extension not allowed: .{}", ext),
            Self::FileNameNotAllowed(name) => write!(f, "File not allowed: {}", name),
            Self::MissingExtension => write!(f, "File has no extension"),
            Self::MissingFileName => write!(f, "Path has no file name"),
        }
    }
}

pub struct ComicFileGuard;
pub struct MetadataFileGuard;
pub struct ArtworkFileGuard;

pub trait FileGuard: Send + Sync {
    fn is_allowed(&self, path: &Path) -> Result<(), FileGuardError>;
}

impl FileGuard for ComicFileGuard {
    fn is_allowed(&self, path: &Path) -> Result<(), FileGuardError> {
        let ext = path
            .extension()
            .and_then(|ext| ext.to_str())
            .ok_or(FileGuardError::MissingExtension)?;

        match ext {
            "cbz" | "cbr" | "pdf" => Ok(()),
            _ => Err(FileGuardError::ExtensionNotAllowed(ext.to_string())),
        }
    }
}

impl FileGuard for MetadataFileGuard {
    fn is_allowed(&self, path: &Path) -> Result<(), FileGuardError> {
        let name = path
            .file_name()
            .and_then(|name| name.to_str())
            .ok_or(FileGuardError::MissingFileName)?;

        match name {
            "ComicInfo.xml" => Ok(()),
            _ => Err(FileGuardError::FileNameNotAllowed(name.to_string())),
        }
    }
}

impl FileGuard for ArtworkFileGuard {
    fn is_allowed(&self, path: &Path) -> Result<(), FileGuardError> {
        let name = path
            .file_name()
            .and_then(|name| name.to_str())
            .ok_or(FileGuardError::MissingFileName)?;

        match name {
            | "cover.png"
            | "cover.jpg"
            | "cover.jpeg"
            | "banner.png"
            | "banner.jpg"
            | "banner.jpeg" => Ok(()),
            _ => Err(FileGuardError::FileNameNotAllowed(name.to_string())),
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
                Box::new(ComicFileGuard),
                Box::new(MetadataFileGuard),
                Box::new(ArtworkFileGuard)
            ],
        }
    }

    pub fn is_allowed(&self, path: &Path) -> Result<(), Vec<FileGuardError>> {
        let errors: Vec<FileGuardError> = self.guards
            .iter()
            .filter_map(|guard| guard.is_allowed(path).err())
            .collect();

        if errors.len() == self.guards.len() {
            Err(errors)
        } else {
            Ok(())
        }
    }
}

#[cfg(test)]
mod tests {
    use super::{ ComicFileGuard, MetadataFileGuard, ArtworkFileGuard, ScannerGuard, FileGuard };
    use std::path::Path;

    // NOTE: ComicFileGuard
    #[test]
    fn teste_comic_extensao_valida() {
        let guard = ComicFileGuard;
        assert!(guard.is_allowed(Path::new("berserk.cbz")).is_ok());
        assert!(guard.is_allowed(Path::new("berserk.cbr")).is_ok());
        assert!(guard.is_allowed(Path::new("berserk.pdf")).is_ok());
    }

    #[test]
    fn teste_comic_extensao_invalida() {
        let guard = ComicFileGuard;
        let result = guard.is_allowed(Path::new("berserk.exe"));

        assert!(result.is_err());
        assert!(result.unwrap_err().to_string().contains(".exe"));
    }

    #[test]
    fn teste_comic_sem_extensao() {
        let guard = ComicFileGuard;
        assert!(guard.is_allowed(Path::new("berserk")).is_err());
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

        assert!(result.is_err());
        assert!(result.unwrap_err().to_string().contains("info.xml"));
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

        assert!(result.is_err());
        assert!(result.unwrap_err().to_string().contains("thumbnail.png"));
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
        assert!(result.is_err());
        assert_eq!(result.unwrap_err().len(), 3); // todos os guards rejeitaram
    }
}
