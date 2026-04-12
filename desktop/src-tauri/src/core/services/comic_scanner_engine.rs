use std::collections::hash_map::DefaultHasher;
use std::hash::{Hash, Hasher};
use std::path::{Path, PathBuf};
use tauri::{AppHandle, Emitter};
use tokio::fs;
use tokio::sync::mpsc;

use crate::data::models::archive::chapter_archive::ChapterArchive;
use crate::data::models::archive::chapter_template::ChapterTemplate;
use crate::data::models::archive::comic_directory::ComicDirectory;
use crate::data::repositories::archive::chapter_archive_repo::ChapterRepository;
use crate::data::repositories::archive::chapter_template_repo::ChapterTemplateRepository;
use crate::data::repositories::archive::comic_directory_repo::ComicRepository;
use crate::infra::error::translations::comic_error::ComicError;
use crate::infra::error::translations::db_error::DbError;
use crate::infra::filesystem::files_guard::{ArchiveFileGuard, ScannerGuard};
use crate::infra::filesystem::files_guard::{ArtworkFileGuard, FileGuard};
use crate::infra::filesystem::path_guard::PathGuard;
use crate::infra::filesystem::scanner_engine::{DirectoryEntry, ScannerEngine};
use crate::infra::pattern::chapter_template::{detect_template, extract_chapter_parts};
use crate::infra::pattern::template_validator::{extract_tags, validate_template};

pub struct ComicScannerService {
    path_guard: PathGuard,
    comic_repo: ComicRepository,
    chapter_repo: ChapterRepository,
    template_repo: ChapterTemplateRepository,
}

impl ComicScannerService {
    pub fn new(root: PathBuf, pool: sqlx::SqlitePool) -> Self {
        Self {
            path_guard: PathGuard::new(root),
            comic_repo: ComicRepository::new(pool.clone()),
            chapter_repo: ChapterRepository::new(pool.clone()),
            template_repo: ChapterTemplateRepository::new(pool.clone()),
        }
    }

    pub async fn scan(&self, path: PathBuf, app: &AppHandle) -> Result<(), ComicError> {
        self.path_guard
            .execute(&path, |_| -> Result<(), String> { Ok(()) })?;

        let (tx, mut rx) = mpsc::channel(32);

        let file_guard = ScannerGuard::new();
        let scanner = ScannerEngine::new();

        tokio::spawn(async move {
            scanner.scan(path, tx).await.unwrap();
        });

        let templates = self.template_repo.base.find_all().await?;

        while let Some(entry) = rx.recv().await {
            let directory = entry.directory.to_string_lossy().to_string();

            // Emite o progresso e qual pasta está sendo escaneada
            self.process_entry(entry, &file_guard, &templates).await?;
            // FIXME: Verificar se dá pra fazer um contrato no command que eu consiga usar
            let _ = app.emit("scan:progress", directory);
        }

        Ok(())
    }

    async fn process_entry(
        &self,
        entry: DirectoryEntry,
        _file_guard: &ScannerGuard,
        templates: &[ChapterTemplate],
    ) -> Result<(), ComicError> {
        let archive_guard = ArchiveFileGuard;
        let artwork_guard = ArtworkFileGuard;

        let mut comic_files: Vec<PathBuf> = vec![];
        let mut banner: Option<String> = None;
        let mut cover: Option<String> = None;

        for file in entry.files {
            let name = file
                .file_name()
                .and_then(|name| name.to_str())
                .unwrap_or("");

            if archive_guard.is_allowed(&file).is_ok() {
                comic_files.push(file);
                continue;
            }

            if artwork_guard.is_allowed(&file).is_ok() && name.starts_with("cover.") {
                cover = Some(file.to_string_lossy().to_string());
                continue;
            }

            if artwork_guard.is_allowed(&file).is_ok() && name.starts_with("banner.") {
                banner = Some(file.to_string_lossy().to_string());
                continue;
            }

            // INFO: ComicInfo.xml, .pdf e outros ignorados por ora
        }

        // NOTE: Só persiste se tiver arquivos de quadrinhos
        if comic_files.is_empty() {
            return Ok(());
        }

        let detected: Option<&ChapterTemplate> = comic_files
            .first()
            .and_then(|file| file.file_name())
            .and_then(|name| name.to_str())
            .and_then(|file_str| {
                let template_strs: Vec<&str> = templates
                    .iter()
                    .map(|template| template.pattern.as_str())
                    .collect();

                detect_template(file_str, &template_strs, |template| {
                    validate_template(template, extract_tags)
                })
            })
            .and_then(|pattern| {
                templates
                    .iter()
                    .find(|template| template.pattern == pattern)
            });

        let template_fk = detected.map(|template| template.id);
        let template_pattern = detected.map(|template| template.pattern.as_str());

        let dir_meta = fs::metadata(&entry.directory).await?;
        let dir_name = entry
            .directory
            .file_name()
            .and_then(|name: &std::ffi::OsStr| name.to_str())
            .unwrap_or("Unknown")
            .to_string();

        // TODO: Fazer o last_modified abrir porta para o Deep sync e fast sync
        let comic = ComicDirectory {
            id: path_hash(&entry.directory),
            name: dir_name,
            path: entry.directory.to_string_lossy().to_string(),
            cover,
            banner,
            last_modified: modified_secs(&dir_meta),
            chapter_template_fk: template_fk,
            external_sync_enabled: false,
            hidden: false,
        };

        let saved = match self.comic_repo.base.insert(&comic).await {
            Ok(saved) => saved,
            Err(DbError::UniqueViolation) => {
                log::debug!(
                    "[Scanner] Comic '{}' already indexed, skipping.",
                    comic.name
                );
                return Ok(());
            }
            Err(err) => {
                return Err(err.into());
            }
        };

        for (index, file) in comic_files.iter().enumerate() {
            self.process_chapter(&file, index, saved.id, template_pattern)
                .await?;
        }

        Ok(())
    }

    async fn process_chapter(
        &self,
        file: &Path,
        index: usize,
        comic_id: i64,
        template: Option<&str>,
    ) -> Result<(), ComicError> {
        let meta = fs::metadata(file).await?;

        let file_name = file
            .file_name()
            .and_then(|it| it.to_str())
            .ok_or_else(|| ComicError::SystemFailure("File name is invalid".into()))?;

        let file_size = meta.len();
        let file_modified = modified_secs(&meta);

        let fast_hash = format!("{}|{}|{}", file_name, file_size, file_modified);

        let chapter_name = file
            .file_stem()
            .and_then(|it| it.to_str())
            .unwrap_or("unknown")
            .to_string();

        let chapter_sort = template
            .and_then(|template| {
                extract_chapter_parts(file_name, template, |template| validate_template(template, extract_tags))
            })
            .map(|(chapter, decimal)| ChapterArchive::format_sort(chapter, decimal))
            .unwrap_or_else(|| ChapterArchive::fallback_sort(&chapter_name, index));

        let chapter = ChapterArchive {
            id: path_hash(file),
            chapter: chapter_name.clone(),
            path: file.to_string_lossy().to_string(),
            chapter_sort: chapter_sort,
            fast_hash: Some(fast_hash),
            comic_directory_fk: comic_id,
            last_modified: file_modified,
        };

        match self.chapter_repo.base.insert(&chapter).await {
            Ok(_) => {}
            Err(DbError::UniqueViolation) => {
                log::debug!(
                    "[Scanner] Chapter '{}' already indexed, skipping.",
                    chapter.chapter
                );
            }
            Err(err) => {
                return Err(err.into());
            }
        }

        Ok(())
    }
}

/// Gera um id determinístico baseado no path — mesmo path sempre gera o mesmo id.
/// Garante que re-escanear o mesmo diretório não crie duplicatas.
fn path_hash(path: &Path) -> i64 {
    let mut hasher = DefaultHasher::new();
    path.hash(&mut hasher);
    (hasher.finish() & 0x7fff_ffff_ffff_ffff) as i64
}

/// Retorna o `last_modified` em segundos desde Unix epoch.
/// TODO: Verificar se a forma de ver o last_modified é igual em linux e windows, ver também se é possivel fazer app funcionar no flatpak
#[rustfmt::skip]
fn modified_secs(meta: &std::fs::Metadata) -> i64 {
    // FIXME: Colocar tratamento de erros
    meta.modified().map(|time: std::time::SystemTime| {
            time.duration_since(std::time::UNIX_EPOCH).unwrap_or_default().as_secs() as i64
        }).unwrap_or(0)
}
