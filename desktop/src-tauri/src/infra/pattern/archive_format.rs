use std::sync::OnceLock;

static EXTENSIONS: OnceLock<String> = OnceLock::new();

#[derive(Debug, PartialEq, Eq)]
pub enum ArchiveFormat {
    Cbz,
    Cbr,
    Pdf,
}

impl ArchiveFormat {
    pub fn extension(&self) -> &'static str {
        match self {
            Self::Cbz => "cbz",
            Self::Cbr => "cbr",
            Self::Pdf => "pdf",
        }
    }

    pub fn all() -> &'static [ArchiveFormat] {
        &[Self::Cbz, Self::Cbr, Self::Pdf]
    }

    pub fn from_extension(extension: &str) -> Option<Self> {
        match extension.to_ascii_lowercase().as_str() {
            "cbz" => Some(Self::Cbz),
            "cbr" => Some(Self::Cbr),
            "pdf" => Some(Self::Pdf),
            _ => None,
        }
    }

    pub fn extensions_pattern() -> &'static str {
        EXTENSIONS.get_or_init(|| {
            Self::all().iter().map(|it| it.extension()).collect::<Vec<_>>().join("|")
        })
    }
}
