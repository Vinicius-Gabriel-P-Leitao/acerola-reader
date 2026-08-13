use std::{
    fs::File,
    io::Read,
    num::NonZeroUsize,
    path::{Path, PathBuf},
    sync::{Arc, Mutex},
};

use lru::LruCache;
use zip::ZipArchive;

use crate::{
    cmd::events::reader::{
        ReaderChapterPayload, ReaderPagePayload, ReaderSessionPayload, ReaderStatusPayload,
    },
    infra::{
        error::ReaderError,
        pattern::{
            archive_format::ArchiveFormat, image_file_format::ImageFileFormat,
            natural_sort::natural_cmp,
        },
    },
};

const DEFAULT_CACHE_CAPACITY: usize = 100;
const DEFAULT_PREFETCH_RADIUS: usize = 2;

#[derive(Clone)]
pub struct ReaderService {
    state: Arc<Mutex<ReaderState>>,
}

impl ReaderService {
    pub fn new() -> Self {
        Self { state: Arc::new(Mutex::new(ReaderState::default())) }
    }

    pub async fn open_chapter(
        &self, chapter: ReaderChapterPayload,
    ) -> Result<ReaderSessionPayload, ReaderError> {
        self.with_state("open_chapter", move |state| {
            let session = ReaderSession::open(chapter)?;
            let payload = session.session_payload();
            state.session = Some(session);
            Ok(payload)
        })
        .await
    }

    pub async fn load_page(
        &self, index: usize, set_current: bool,
    ) -> Result<ReaderPagePayload, ReaderError> {
        self.with_state("load_page", move |state| {
            let session = state.session.as_mut().ok_or(ReaderError::ChapterNotOpen)?;
            session.load_page(index, set_current)
        })
        .await
    }

    pub async fn set_current_page(&self, index: usize) -> Result<ReaderStatusPayload, ReaderError> {
        self.with_state("set_current_page", move |state| {
            let session = state.session.as_mut().ok_or(ReaderError::ChapterNotOpen)?;
            session.set_current_page(index)?;
            Ok(state.status())
        })
        .await
    }

    pub async fn status(&self) -> Result<ReaderStatusPayload, ReaderError> {
        self.with_state("status", |state| Ok(state.status())).await
    }

    pub async fn close_chapter(&self) -> Result<ReaderStatusPayload, ReaderError> {
        self.with_state("close_chapter", |state| {
            state.session = None;
            Ok(state.status())
        })
        .await
    }

    pub fn prefetch_window_background(&self, center: usize, radius: Option<usize>) {
        let service = self.clone();
        let radius = radius.unwrap_or(DEFAULT_PREFETCH_RADIUS);

        tokio::spawn(async move {
            if let Err(err) = service.prefetch_window(center, radius).await {
                tracing::warn!(
                    center,
                    radius,
                    error = %err,
                    "[reader] prefetch window failed"
                );
            }
        });
    }

    async fn prefetch_window(&self, center: usize, radius: usize) -> Result<(), ReaderError> {
        self.with_state("prefetch_window", move |state| {
            let session = state.session.as_mut().ok_or(ReaderError::ChapterNotOpen)?;
            session.prefetch_window(center, radius)
        })
        .await
    }

    async fn with_state<F, R>(
        &self, operation_name: &'static str, operation: F,
    ) -> Result<R, ReaderError>
    where
        F: FnOnce(&mut ReaderState) -> Result<R, ReaderError> + Send + 'static,
        R: Send + 'static,
    {
        let state = Arc::clone(&self.state);

        let task_result = tokio::task::spawn_blocking(move || {
            let mut guard = match state.lock() {
                Ok(guard) => guard,
                Err(err) => {
                    tracing::error!(
                        operation = operation_name,
                        error = %err,
                        "[reader] state lock poisoned"
                    );

                    return Err(ReaderError::SystemFailure(format!(
                        "reader state lock poisoned during {operation_name}"
                    )));
                },
            };

            operation(&mut guard)
        })
        .await;

        match task_result {
            Ok(Ok(value)) => Ok(value),
            Ok(Err(err)) => Err(err),
            Err(err) => {
                tracing::error!(
                    operation = operation_name,
                    is_cancelled = err.is_cancelled(),
                    is_panic = err.is_panic(),
                    error = %err,
                    "[reader] blocking state task failed"
                );

                Err(ReaderError::SystemFailure(format!(
                    "reader state task failed during {operation_name}"
                )))
            },
        }
    }
}

impl Default for ReaderService {
    fn default() -> Self {
        Self::new()
    }
}

#[derive(Default)]
struct ReaderState {
    session: Option<ReaderSession>,
}

impl ReaderState {
    fn status(&self) -> ReaderStatusPayload {
        match &self.session {
            Some(session) => session.status_payload(),
            None => ReaderStatusPayload {
                is_open: false,
                chapter_id: None,
                page_count: 0,
                current_page: None,
                cache_keys: Vec::new(),
                cache_capacity: DEFAULT_CACHE_CAPACITY,
            },
        }
    }
}

struct ReaderSession {
    chapter: ReaderChapterPayload,
    source: Box<dyn PageSource>,
    cache: LruCache<usize, Arc<CachedPage>>,
    current_page: usize,
}

impl ReaderSession {
    fn open(chapter: ReaderChapterPayload) -> Result<Self, ReaderError> {
        let path = PathBuf::from(&chapter.path);
        let source = source_from_path(&path)?;

        Ok(Self {
            chapter,
            source,
            cache: LruCache::new(NonZeroUsize::new(DEFAULT_CACHE_CAPACITY).unwrap()),
            current_page: 0,
        })
    }

    fn session_payload(&self) -> ReaderSessionPayload {
        ReaderSessionPayload {
            chapter: self.chapter.clone(),
            page_count: self.total_pages(),
            current_page: self.current_page,
            cache_capacity: self.cache.cap().get(),
        }
    }

    fn status_payload(&self) -> ReaderStatusPayload {
        ReaderStatusPayload {
            is_open: true,
            chapter_id: Some(self.chapter.id.clone()),
            page_count: self.total_pages(),
            current_page: Some(self.current_page),
            cache_keys: self.cache.iter().map(|(key, _)| *key).collect(),
            cache_capacity: self.cache.cap().get(),
        }
    }

    fn total_pages(&self) -> usize {
        self.source.page_count().get()
    }

    fn set_current_page(&mut self, index: usize) -> Result<(), ReaderError> {
        self.ensure_index(index)?;
        self.current_page = index;

        if self.cache.contains(&index) {
            self.cache.get(&index);
        }

        Ok(())
    }

    fn load_page(
        &mut self, index: usize, set_current: bool,
    ) -> Result<ReaderPagePayload, ReaderError> {
        self.ensure_index(index)?;

        if set_current {
            self.current_page = index;
        }

        let (page, cache_hit) = self.cached_page(index)?;

        Ok(ReaderPagePayload {
            chapter_id: self.chapter.id.clone(),
            index,
            total: self.total_pages(),
            mime_type: page.mime_type.clone(),
            bytes: page.bytes.clone(),
            cache_hit,
        })
    }

    fn prefetch_window(&mut self, center: usize, radius: usize) -> Result<(), ReaderError> {
        self.ensure_index(center)?;

        for index in window_indices(center, self.total_pages(), radius) {
            self.ensure_cached(index)?;
        }

        Ok(())
    }

    fn cached_page(&mut self, index: usize) -> Result<(Arc<CachedPage>, bool), ReaderError> {
        if let Some(page) = self.cache.get(&index) {
            return Ok((Arc::clone(page), true));
        }

        let page = Arc::new(self.read_page(index)?);
        self.cache.put(index, Arc::clone(&page));

        Ok((page, false))
    }

    fn ensure_cached(&mut self, index: usize) -> Result<(), ReaderError> {
        if self.cache.contains(&index) {
            return Ok(());
        }

        let page = Arc::new(self.read_page(index)?);
        self.cache.put(index, page);
        Ok(())
    }

    fn read_page(&self, index: usize) -> Result<CachedPage, ReaderError> {
        let raw = self.source.read_page(index)?;
        let mime_type = mime_type_for(&raw.name)?;

        Ok(CachedPage { mime_type, bytes: raw.bytes })
    }

    fn ensure_index(&self, index: usize) -> Result<(), ReaderError> {
        let total = self.total_pages();

        if index >= total {
            return Err(ReaderError::PageOutOfBounds { index, total });
        }

        Ok(())
    }
}

struct CachedPage {
    mime_type: String,
    bytes: Vec<u8>,
}

struct RawPage {
    name: String,
    bytes: Vec<u8>,
}

trait PageSource: Send {
    fn page_count(&self) -> NonZeroUsize;
    fn read_page(&self, index: usize) -> Result<RawPage, ReaderError>;
}

struct CbzPageSource {
    path: PathBuf,
    entries: Vec<String>,
    page_count: NonZeroUsize,
}

impl CbzPageSource {
    fn open(path: &Path) -> Result<Self, ReaderError> {
        let file = File::open(path)?;
        let mut archive = ZipArchive::new(file)?;
        let mut entries = Vec::new();

        for index in 0..archive.len() {
            let entry = archive.by_index(index)?;
            let name = entry.name().to_string();

            if entry.is_file() && is_supported_page_image_name(&name) {
                entries.push(name);
            }
        }

        sort_page_names(&mut entries);
        let page_count = page_count_from_len(path, entries.len())?;

        Ok(Self { path: path.to_path_buf(), entries, page_count })
    }
}

impl PageSource for CbzPageSource {
    fn page_count(&self) -> NonZeroUsize {
        self.page_count
    }

    fn read_page(&self, index: usize) -> Result<RawPage, ReaderError> {
        let name = self
            .entries
            .get(index)
            .ok_or(ReaderError::PageOutOfBounds { index, total: self.page_count.get() })?;

        let file = File::open(&self.path)?;
        let mut archive = ZipArchive::new(file)?;
        let mut entry = archive.by_name(name).map_err(|err| match err {
            zip::result::ZipError::FileNotFound => {
                ReaderError::PageEntryMissing { index, name: name.clone() }
            },
            err => ReaderError::from(err),
        })?;
        let mut bytes = Vec::new();
        entry.read_to_end(&mut bytes)?;

        Ok(RawPage { name: name.clone(), bytes })
    }
}

struct CbrPageSource {
    path: PathBuf,
    entries: Vec<PathBuf>,
    page_count: NonZeroUsize,
}

impl CbrPageSource {
    fn open(path: &Path) -> Result<Self, ReaderError> {
        let archive = unrar::Archive::new(path).open_for_listing()?;
        let mut entries = Vec::new();

        for entry in archive {
            let entry = entry?;

            if entry.is_file() && is_supported_page_image_path(&entry.filename) {
                entries.push(entry.filename);
            }
        }

        sort_page_paths(&mut entries);
        let page_count = page_count_from_len(path, entries.len())?;

        Ok(Self { path: path.to_path_buf(), entries, page_count })
    }
}

impl PageSource for CbrPageSource {
    fn page_count(&self) -> NonZeroUsize {
        self.page_count
    }

    fn read_page(&self, index: usize) -> Result<RawPage, ReaderError> {
        let target = self
            .entries
            .get(index)
            .ok_or(ReaderError::PageOutOfBounds { index, total: self.page_count.get() })?;

        let mut archive = unrar::Archive::new(&self.path).open_for_processing()?;

        loop {
            let Some(entry) = archive.read_header()? else {
                break;
            };

            if &entry.entry().filename == target {
                let name = target.to_string_lossy().to_string();
                let (bytes, _) = entry.read()?;
                return Ok(RawPage { name, bytes });
            }

            archive = entry.skip()?;
        }

        Err(ReaderError::PageEntryMissing { index, name: target.to_string_lossy().to_string() })
    }
}

fn source_from_path(path: &Path) -> Result<Box<dyn PageSource>, ReaderError> {
    if !path.exists() {
        return Err(std::io::Error::new(
            std::io::ErrorKind::NotFound,
            path.to_string_lossy().to_string(),
        )
        .into());
    }

    let extension = path
        .extension()
        .and_then(|extension| extension.to_str())
        .unwrap_or("")
        .to_ascii_lowercase();

    match ArchiveFormat::from_extension(&extension) {
        Some(ArchiveFormat::Cbz) => Ok(Box::new(CbzPageSource::open(path)?)),
        Some(ArchiveFormat::Cbr) => Ok(Box::new(CbrPageSource::open(path)?)),
        Some(ArchiveFormat::Pdf) => Err(ReaderError::UnsupportedFormat(extension)),
        None => Err(ReaderError::UnsupportedFormat(extension)),
    }
}

fn window_indices(center: usize, total: usize, radius: usize) -> Vec<usize> {
    if total == 0 {
        return Vec::new();
    }

    let start = center.saturating_sub(radius);
    let end = center.saturating_add(radius).min(total - 1);

    (start..=end).collect()
}

fn is_supported_page_image_name(name: &str) -> bool {
    ImageFileFormat::from_path(Path::new(name)).is_some()
}

fn is_supported_page_image_path(path: &Path) -> bool {
    ImageFileFormat::from_path(path).is_some()
}

fn page_count_from_len(path: &Path, len: usize) -> Result<NonZeroUsize, ReaderError> {
    NonZeroUsize::new(len)
        .ok_or_else(|| ReaderError::EmptyChapter(path.to_string_lossy().to_string()))
}

fn sort_page_names(entries: &mut [String]) {
    entries.sort_by(|left, right| natural_cmp(left, right));
}

fn sort_page_paths(entries: &mut [PathBuf]) {
    entries.sort_by(|left, right| natural_cmp(&left.to_string_lossy(), &right.to_string_lossy()));
}

fn mime_type_for(name: &str) -> Result<String, ReaderError> {
    ImageFileFormat::from_path(Path::new(name))
        .map(|format| format.mime_type().to_string())
        .ok_or_else(|| ReaderError::Image(name.to_string()))
}

#[cfg(test)]
mod tests {
    use std::{fs::File, io::Write, path::Path};

    use tempfile::TempDir;
    use zip::{
        write::{SimpleFileOptions, ZipWriter},
        CompressionMethod,
    };

    use super::{window_indices, ReaderError, ReaderService};
    use crate::{cmd::events::reader::ReaderChapterPayload, infra::pattern::natural_sort::natural_key};

    fn chapter(path: &Path) -> ReaderChapterPayload {
        ReaderChapterPayload {
            id: "chapter-1".to_string(),
            name: "Chapter 1".to_string(),
            path: path.to_string_lossy().to_string(),
            chapter_sort: "1".to_string(),
            volume_id: Some("volume-1".to_string()),
            volume_name: Some("Volume 1".to_string()),
            is_special: false,
            last_modified: 0,
        }
    }

    fn create_cbz(temp_dir: &TempDir, name: &str, entries: &[(&str, &[u8])]) -> std::path::PathBuf {
        let path = temp_dir.path().join(name);
        let file = File::create(&path).unwrap();
        let mut archive = ZipWriter::new(file);
        let options = SimpleFileOptions::default().compression_method(CompressionMethod::Stored);

        for (entry_name, bytes) in entries {
            archive.start_file(*entry_name, options).unwrap();
            archive.write_all(bytes).unwrap();
        }

        archive.finish().unwrap();
        path
    }

    #[test]
    fn teste_ordena_paginas_numeradas_por_ordem_natural() {
        let mut pages = vec!["page10.jpg", "page2.jpg", "page001.jpg"];
        pages.sort_by_key(|page| natural_key(page));

        assert_eq!(pages, vec!["page001.jpg", "page2.jpg", "page10.jpg"]);
    }

    #[test]
    fn teste_intervalo_de_paginas_respeita_limites() {
        assert_eq!(window_indices(0, 5, 2), vec![0, 1, 2]);
        assert_eq!(window_indices(4, 5, 2), vec![2, 3, 4]);
    }

    #[test]
    fn teste_intervalo_de_paginas_com_total_zero_retorna_vazio() {
        assert_eq!(window_indices(0, 0, 2), Vec::<usize>::new());
    }

    #[tokio::test]
    async fn teste_abre_cbz_e_carrega_paginas_em_ordem_natural() {
        let temp_dir = TempDir::new().unwrap();
        let cbz_path = create_cbz(
            &temp_dir,
            "chapter.cbz",
            &[("page10.jpg", &[10]), ("page2.png", &[2]), ("page001.jpg", &[1])],
        );
        let reader = ReaderService::new();

        let session = reader.open_chapter(chapter(&cbz_path)).await.unwrap();

        assert_eq!(session.chapter.id, "chapter-1");
        assert_eq!(session.page_count, 3);
        assert_eq!(session.current_page, 0);
        assert_eq!(session.cache_capacity, 100);

        let first_page = reader.load_page(0, true).await.unwrap();
        assert_eq!(first_page.chapter_id, "chapter-1");
        assert_eq!(first_page.index, 0);
        assert_eq!(first_page.total, 3);
        assert_eq!(first_page.mime_type, "image/jpeg");
        assert_eq!(first_page.bytes, vec![1]);
        assert!(!first_page.cache_hit);

        let second_page = reader.load_page(1, true).await.unwrap();
        assert_eq!(second_page.mime_type, "image/png");
        assert_eq!(second_page.bytes, vec![2]);

        let status = reader.status().await.unwrap();
        assert_eq!(status.current_page, Some(1));
        assert_eq!(status.page_count, 3);
    }

    #[tokio::test]
    async fn teste_reutiliza_cache_ao_carregar_mesma_pagina() {
        let temp_dir = TempDir::new().unwrap();
        let cbz_path = create_cbz(&temp_dir, "chapter.cbz", &[("001.jpg", &[1])]);
        let reader = ReaderService::new();

        reader.open_chapter(chapter(&cbz_path)).await.unwrap();

        let first_read = reader.load_page(0, false).await.unwrap();
        assert!(!first_read.cache_hit);

        let second_read = reader.load_page(0, false).await.unwrap();
        assert!(second_read.cache_hit);
        assert_eq!(second_read.bytes, vec![1]);
    }

    #[tokio::test]
    async fn teste_altera_pagina_atual_sem_carregar_cache() {
        let temp_dir = TempDir::new().unwrap();
        let cbz_path = create_cbz(
            &temp_dir,
            "chapter.cbz",
            &[("001.jpg", &[1]), ("002.jpg", &[2]), ("003.jpg", &[3])],
        );
        let reader = ReaderService::new();

        reader.open_chapter(chapter(&cbz_path)).await.unwrap();

        let status = reader.set_current_page(2).await.unwrap();
        assert_eq!(status.current_page, Some(2));
        assert_eq!(status.cache_keys, Vec::<usize>::new());
    }

    #[tokio::test]
    async fn teste_pre_carregamento_carrega_paginas_vizinhas() {
        let temp_dir = TempDir::new().unwrap();
        let cbz_path = create_cbz(
            &temp_dir,
            "chapter.cbz",
            &[
                ("001.jpg", &[1]),
                ("002.jpg", &[2]),
                ("003.jpg", &[3]),
                ("004.jpg", &[4]),
                ("005.jpg", &[5]),
            ],
        );
        let reader = ReaderService::new();

        reader.open_chapter(chapter(&cbz_path)).await.unwrap();
        reader.prefetch_window(2, 1).await.unwrap();

        let mut cache_keys = reader.status().await.unwrap().cache_keys;
        cache_keys.sort_unstable();
        assert_eq!(cache_keys, vec![1, 2, 3]);
    }

    #[tokio::test]
    async fn teste_fecha_capitulo_e_limpa_status() {
        let temp_dir = TempDir::new().unwrap();
        let cbz_path = create_cbz(&temp_dir, "chapter.cbz", &[("001.jpg", &[1])]);
        let reader = ReaderService::new();

        reader.open_chapter(chapter(&cbz_path)).await.unwrap();
        reader.load_page(0, true).await.unwrap();

        let status = reader.close_chapter().await.unwrap();
        assert!(!status.is_open);
        assert_eq!(status.chapter_id, None);
        assert_eq!(status.page_count, 0);
        assert_eq!(status.current_page, None);
        assert_eq!(status.cache_keys, Vec::<usize>::new());
    }

    #[tokio::test]
    async fn teste_carregar_pagina_sem_capitulo_aberto_retorna_erro() {
        let reader = ReaderService::new();
        let error = reader.load_page(0, true).await.unwrap_err();

        assert!(matches!(error, ReaderError::ChapterNotOpen));
    }

    #[tokio::test]
    async fn teste_carregar_pagina_fora_do_limite_retorna_erro() {
        let temp_dir = TempDir::new().unwrap();
        let cbz_path = create_cbz(&temp_dir, "chapter.cbz", &[("001.jpg", &[1])]);
        let reader = ReaderService::new();

        reader.open_chapter(chapter(&cbz_path)).await.unwrap();
        let error = reader.load_page(1, true).await.unwrap_err();

        assert!(matches!(error, ReaderError::PageOutOfBounds { index: 1, total: 1 }));
    }

    #[tokio::test]
    async fn teste_abrir_capitulo_rejeita_formato_nao_suportado() {
        let temp_dir = TempDir::new().unwrap();
        let path = temp_dir.path().join("chapter.txt");
        std::fs::write(&path, "content").unwrap();
        let reader = ReaderService::new();

        let error = reader.open_chapter(chapter(&path)).await.unwrap_err();

        assert!(matches!(error, ReaderError::UnsupportedFormat(format) if format == "txt"));
    }

    #[tokio::test]
    async fn teste_abrir_capitulo_rejeita_cbz_sem_imagens() {
        let temp_dir = TempDir::new().unwrap();
        let cbz_path = create_cbz(&temp_dir, "chapter.cbz", &[("notes.txt", b"without image")]);
        let reader = ReaderService::new();

        let error = reader.open_chapter(chapter(&cbz_path)).await.unwrap_err();

        assert!(matches!(error, ReaderError::EmptyChapter(path) if path.ends_with("chapter.cbz")));
    }
}
