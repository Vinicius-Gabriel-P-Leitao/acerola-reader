use std::{
    fs::File,
    io::{Cursor, Write},
    path::PathBuf,
};

use image::ImageFormat;
use pdfium::{PdfiumDocument, PdfiumRenderConfig, PdfiumColor, PdfiumBitmapFormat};
use zip::{write::SimpleFileOptions, CompressionMethod, ZipWriter};

use crate::infra::error::ComicError;

/// Serviço especializado na conversão de documentos PDF para o formato CBZ (Comic Book Zip).
pub struct ConverterService;

impl ConverterService {
    pub fn new() -> Self {
        Self
    }

    /// Converte um arquivo PDF localizado em `pdf_path` para um arquivo `.cbz`.
    pub async fn convert_pdf_to_cbz(&self, pdf_path: PathBuf) -> Result<PathBuf, ComicError> {
        let cbz_path = pdf_path.with_extension("cbz");

        tokio::task::spawn_blocking(move || {
            let document = PdfiumDocument::new_from_path(&pdf_path, None)
                .map_err(|error| ComicError::SystemFailure(format!("Failed to load PDF: {}", error)))?;

            let archive_file = File::create(&cbz_path).map_err(ComicError::Io)?;
            let mut zip_writer = ZipWriter::new(archive_file);
            
            let zip_options = SimpleFileOptions::default()
                .compression_method(CompressionMethod::Stored);

            let page_count = document.page_count();
            let render_scale = 2.0;

            let mut image_buffer = Vec::with_capacity(1024 * 1024 * 2);

            for page_index in 0..page_count {
                let page = document.page(page_index)
                    .map_err(|error| ComicError::SystemFailure(format!("Failed to load page {}: {}", page_index, error)))?;

                let page_boundaries = page.boundaries().media().map_err(|error| {
                    ComicError::SystemFailure(format!("Failed to get boundaries for page {}: {}", page_index, error))
                })?;

                let target_width = (page_boundaries.width() * render_scale) as i32;
                
                let render_config = PdfiumRenderConfig::new()
                    .with_width(target_width)
                    .with_format(PdfiumBitmapFormat::Bgra)
                    .with_background(PdfiumColor::WHITE)
                    .with_scale(render_scale);

                let bitmap = page.render(&render_config)
                    .map_err(|error| ComicError::SystemFailure(format!("Failed to render page {}: {}", page_index, error)))?;

                image_buffer.clear();
                let rgba_data = bitmap.as_rgba_bytes().map_err(|error| {
                    ComicError::SystemFailure(format!("Failed to get RGBA bytes: {}", error))
                })?;

                let rgba_image = image::RgbaImage::from_raw(
                    bitmap.width() as u32,
                    bitmap.height() as u32,
                    rgba_data,
                ).ok_or_else(|| ComicError::SystemFailure("Failed to create RgbaImage".into()))?;

                rgba_image.write_to(&mut Cursor::new(&mut image_buffer), ImageFormat::Png)
                    .map_err(|error| ComicError::SystemFailure(format!("Failed to encode PNG: {}", error)))?;

                let entry_name = format!("{:03}.png", page_index + 1);
                zip_writer.start_file(entry_name, zip_options)
                    .map_err(|error| ComicError::SystemFailure(format!("Failed to start zip entry: {}", error)))?;
                
                zip_writer.write_all(&image_buffer).map_err(ComicError::Io)?;
                
                drop(bitmap);
                drop(page);
            }

            zip_writer.finish().map_err(|error| {
                ComicError::SystemFailure(format!("Failed to finalize CBZ archive: {}", error))
            })?;

            Ok(cbz_path)
        })
        .await
        .map_err(|error| {
            ComicError::SystemFailure(format!("Conversion task panicked: {}", error))
        })?
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::PathBuf;

    #[tokio::test]
    async fn test_convert_pdf_to_cbz() {
        let manifest_dir = std::env::var("CARGO_MANIFEST_DIR").unwrap_or_else(|_| ".".to_string());
        let bin_path = std::path::Path::new(&manifest_dir).join(".bin");
        pdfium::set_library_location(bin_path.to_str().unwrap_or("."));

        let converter = ConverterService::new();
        // Resolve the path correctly
        let pdf_path = std::path::Path::new(&manifest_dir).parent().unwrap().join("tests/wdio/comic/pdf/witchcraft.pdf");
        
        let cbz_path = pdf_path.with_extension("cbz");
        if cbz_path.exists() {
            std::fs::remove_file(&cbz_path).unwrap();
        }

        let result = converter.convert_pdf_to_cbz(pdf_path).await;
        assert!(result.is_ok(), "Conversion failed: {:?}", result.err());

        // Validate the generated ZIP
        let file = std::fs::File::open(&cbz_path).unwrap();
        let mut archive = zip::ZipArchive::new(file).expect("Failed to read generated ZIP");
        assert!(archive.len() > 0, "ZIP archive is empty");
        let first_file = archive.by_index(0).unwrap();
        assert_eq!(first_file.name(), "001.png");
    }
}

