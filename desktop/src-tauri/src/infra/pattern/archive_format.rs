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

    pub fn from_extension(ext: &str) -> Option<Self> {
        match ext {
            "cbz" => Some(Self::Cbz),
            "cbr" => Some(Self::Cbr),
            "pdf" => Some(Self::Pdf),
            _ => None,
        }
    }

    pub fn all() -> &'static [ArchiveFormat] {
        &[Self::Cbz, Self::Cbr, Self::Pdf]
    }
}
