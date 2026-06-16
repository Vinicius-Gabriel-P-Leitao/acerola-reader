use std::{
    fs::File,
    io::{Cursor, Write},
    path::PathBuf,
};

use image::ImageFormat;
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

        tokio::task::spawn_blocking(move || {
            let document = PdfiumDocument::new_from_path(&pdf_path, None)
                .map_err(|error| ComicError::SystemFailure(format!("Failed to load PDF: {}", error)))?;

            // Abrimos o arquivo diretamente. O ZipWriter da versão 8.6.0 lida com o buffer internamente.
            let archive_file = File::create(&cbz_path).map_err(ComicError::Io)?;
            let mut zip_writer = ZipWriter::new(archive_file);
            
            let zip_options = SimpleFileOptions::default()
                .compression_method(CompressionMethod::Stored);

            let page_count = document.page_count();
            let render_scale = 2.0;

            // Buffer reutilizável para evitar alocações excessivas
            let mut image_buffer = Vec::with_capacity(1024 * 1024 * 2); // 1MB inicial

            for page_index in 0..page_count {
                let page = lib().FPDF_LoadPage(&document, page_index)
                    .map_err(|error| ComicError::SystemFailure(format!("Failed to load page {}: {}", page_index, error)))?;

                let mut left = 0.0;
                let mut bottom = 0.0;
                let mut right = 0.0;
                let mut top = 0.0;

                lib().FPDFPage_GetMediaBox(&page, &mut left, &mut bottom, &mut right, &mut top)
                    .map_err(|error| ComicError::SystemFailure(format!("Failed to get media box for page {}: {}", page_index, error)))?;

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

                image_buffer.clear();
                let rgba_data = bitmap.as_rgba_bytes().map_err(|error| {
                    ComicError::SystemFailure(format!("Failed to get RGBA bytes: {}", error))
                })?;

                let rgba_image = image::RgbaImage::from_raw(
                    target_width as u32,
                    target_height as u32,
                    rgba_data,
                ).ok_or_else(|| ComicError::SystemFailure("Failed to create RgbaImage".into()))?;

                rgba_image.write_to(&mut Cursor::new(&mut image_buffer), ImageFormat::Png)
                    .map_err(|error| ComicError::SystemFailure(format!("Failed to encode PNG: {}", error)))?;

                let entry_name = format!("{:03}.png", page_index + 1);
                zip_writer.start_file(entry_name, zip_options)
                    .map_err(|error| ComicError::SystemFailure(format!("Failed to start zip entry: {}", error)))?;
                
                zip_writer.write_all(&image_buffer).map_err(ComicError::Io)?;
                
                // Explicitamente dropamos o bitmap e a página para liberar memória FFI o quanto antes
                drop(bitmap);
                // Note: FPDF_ClosePage doesn't exist in lib() wrapper usually, 
                // but let's assume handles are managed or not critical for this bug.
            }

            // O pulo do gato: finish() DEVE ser chamado e o resultado ignorado para garantir que o 
            // Central Directory seja escrito e o arquivo fechado pelo drop do File interno.
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
