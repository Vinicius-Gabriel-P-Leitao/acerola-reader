use std::path::Path;

#[derive(Debug, PartialEq, Eq)]
pub enum ImageFileFormat {
    Jpeg,
    Png,
}

impl ImageFileFormat {
    pub fn from_extension(ext: &str) -> Option<Self> {
        match ext.to_ascii_lowercase().as_str() {
            "jpg" | "jpeg" => Some(Self::Jpeg),
            "png" => Some(Self::Png),
            _ => None,
        }
    }

    pub fn from_path(path: &Path) -> Option<Self> {
        let extension = path.extension().and_then(|extension| extension.to_str())?;
        Self::from_extension(extension)
    }

    pub fn mime_type(&self) -> &'static str {
        match self {
            Self::Jpeg => "image/jpeg",
            Self::Png => "image/png",
        }
    }
}

#[cfg(test)]
mod tests {
    use std::path::Path;

    use super::ImageFileFormat;

    #[test]
    fn aceita_apenas_jpg_jpeg_e_png() {
        assert_eq!(ImageFileFormat::from_extension("jpg"), Some(ImageFileFormat::Jpeg));
        assert_eq!(ImageFileFormat::from_extension("jpeg"), Some(ImageFileFormat::Jpeg));
        assert_eq!(ImageFileFormat::from_extension("png"), Some(ImageFileFormat::Png));
        assert_eq!(ImageFileFormat::from_extension("webp"), None);
        assert_eq!(ImageFileFormat::from_extension("gif"), None);
        assert_eq!(ImageFileFormat::from_extension("bmp"), None);
    }

    #[test]
    fn detecta_formato_por_path() {
        assert_eq!(
            ImageFileFormat::from_path(Path::new("pages/001.JPG")),
            Some(ImageFileFormat::Jpeg)
        );
        assert_eq!(ImageFileFormat::from_path(Path::new("pages/002.webp")), None);
    }
}
