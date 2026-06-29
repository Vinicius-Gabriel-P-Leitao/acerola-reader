use std::{
    fs::File,
    io::{Cursor, Write},
    path::PathBuf,
    time::Instant,
};

use image::codecs::jpeg::JpegEncoder;
use image::ImageEncoder;
use pdfium::{lib, pdfium_types, PdfiumDocument};
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

        // Idempotência: se já existe, retorna direto
        if cbz_path.exists() {
            return Ok(cbz_path);
        }

        tokio::task::spawn_blocking(move || {
            let document = PdfiumDocument::new_from_path(&pdf_path, None)
                .map_err(|error| ComicError::SystemFailure(format!("Failed to load PDF: {}", error)))?;

            let temp_path = cbz_path.with_extension("cbz.tmp");
            let _ = std::fs::remove_file(&temp_path);

            let archive_file = File::create(&temp_path).map_err(ComicError::Io)?;
            let mut zip_writer = ZipWriter::new(archive_file);
            
            let zip_options = SimpleFileOptions::default()
                .compression_method(CompressionMethod::Stored);

            let page_count = document.page_count();
            let render_scale = 2.0;
            let started_at = Instant::now();

            tracing::info!(
                pdf = %pdf_path.to_string_lossy(),
                total_pages = page_count,
                "Starting PDF to CBZ conversion"
            );

            // Buffer reutilizável para evitar alocações excessivas
            let mut image_buffer = Vec::with_capacity(1024 * 1024 * 2); // 2MB inicial

            for page_index in 0..page_count {
                if page_index == 0 || page_index + 1 == page_count || page_index % 25 == 0 {
                    tracing::info!(
                        pdf = %pdf_path.to_string_lossy(),
                        page = page_index + 1,
                        total_pages = page_count,
                        elapsed_ms = started_at.elapsed().as_millis(),
                        "Rendering PDF page"
                    );
                }

                let (target_width, target_height, rgba_data) = {
                    let page = lib().FPDF_LoadPage(&document, page_index)
                        .map_err(|error| ComicError::SystemFailure(format!("Failed to load page {}: {}", page_index, error)))?;

                    let mut left = 0.0;
                    let mut bottom = 0.0;
                    let mut right = 0.0;
                    let mut top = 0.0;

                    if lib().FPDFPage_GetMediaBox(&page, &mut left, &mut bottom, &mut right, &mut top).is_err() {
                        right = lib().FPDF_GetPageWidthF(&page) as f32;
                        if right == 0.0 { right = lib().FPDF_GetPageWidth(&page) as f32; }
                        top = lib().FPDF_GetPageHeightF(&page) as f32;
                        if top == 0.0 { top = lib().FPDF_GetPageHeight(&page) as f32; }
                    }

                    let target_width = ((right - left).abs() * render_scale) as i32;
                    let target_height = ((top - bottom).abs() * render_scale) as i32;

                    let bitmap = lib().FPDFBitmap_Create(target_width, target_height, 1)
                        .map_err(|error| ComicError::SystemFailure(format!("Failed to create bitmap for page {}: {}", page_index, error)))?;

                    lib().FPDFBitmap_FillRect(&bitmap, 0, 0, target_width, target_height, 0xffffffff)
                        .map_err(|error| ComicError::SystemFailure(format!("Failed to clear bitmap for page {}: {}", page_index, error)))?;

                    let transform_matrix = pdfium_types::FS_MATRIX {
                        a: render_scale,
                        b: 0.0,
                        c: 0.0,
                        d: render_scale,
                        e: 0.0,
                        f: 0.0,
                    };

                    let clipping_rect = pdfium_types::FS_RECTF {
                        left: 0.0,
                        top: target_height as f32,
                        right: target_width as f32,
                        bottom: 0.0,
                    };

                    lib().FPDF_RenderPageBitmapWithMatrix(
                        &bitmap,
                        &page,
                        &transform_matrix,
                        &clipping_rect,
                        0,
                    );

                    let rgba = bitmap.as_rgba_bytes().map_err(|error| {
                        ComicError::SystemFailure(format!("Failed to get RGBA bytes: {}", error))
                    })?;

                    let cloned_rgba = rgba.to_vec();

                    drop(bitmap);
                    // lib().FPDF_ClosePage(&page); // Se precisasse de fechar manualmente

                    (target_width, target_height, cloned_rgba)
                };

                // Agora fora do lock, podemos fazer o encode do JPEG paralelizadamente (livre de FFI)
                image_buffer.clear();

                // Converte RGBA para RGB
                let rgb_data: Vec<u8> = rgba_data
                    .chunks_exact(4)
                    .flat_map(|px| [px[0], px[1], px[2]])
                    .collect();

                let encoder = JpegEncoder::new_with_quality(
                    Cursor::new(&mut image_buffer),
                    90, // JPEG de alta qualidade e super rápido
                );

                encoder
                    .write_image(&rgb_data, target_width as u32, target_height as u32, image::ExtendedColorType::Rgb8)
                    .map_err(|error| ComicError::SystemFailure(format!("Failed to encode JPEG: {}", error)))?;

                let entry_name = format!("{:03}.jpg", page_index + 1);
                zip_writer.start_file(entry_name, zip_options)
                    .map_err(|error| ComicError::SystemFailure(format!("Failed to start zip entry: {}", error)))?;
                
                zip_writer.write_all(&image_buffer).map_err(ComicError::Io)?;
            }

            zip_writer.finish().map_err(|error| {
                ComicError::SystemFailure(format!("Failed to finalize CBZ archive: {}", error))
            })?;

            match std::fs::rename(&temp_path, &cbz_path) {
                Ok(_) => {},
                Err(_error) if cbz_path.exists() => {
                    std::fs::remove_file(&cbz_path).map_err(ComicError::Io)?;
                    std::fs::rename(&temp_path, &cbz_path).map_err(|rename_error| {
                        ComicError::SystemFailure(format!("Failed to rename CBZ: {}", rename_error))
                    })?;
                },
                Err(error) => return Err(ComicError::Io(error)),
            }

            tracing::info!(
                pdf = %pdf_path.to_string_lossy(),
                cbz = %cbz_path.to_string_lossy(),
                elapsed_ms = started_at.elapsed().as_millis(),
                "Finished PDF to CBZ conversion"
            );

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
    use std::io::Read;

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
        assert!(!archive.is_empty(), "ZIP archive is empty");
        let mut first_file = archive.by_index(0).unwrap();
        assert_eq!(first_file.name(), "001.jpg");
        
        let mut bytes = Vec::new();
        first_file.read_to_end(&mut bytes).unwrap();
        assert!(!bytes.is_empty(), "First rendered page is empty");
    }
}
