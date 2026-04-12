use std::collections::{HashMap, HashSet};
use std::path::{Path, PathBuf};
use tokio::fs;
use tokio::sync::mpsc;

use crate::core::services::chapter_scanner_engine::ChapterScannerService;
use crate::data::models::archive::chapter_template::ChapterTemplate;
use crate::data::models::archive::comic_directory::ComicDirectory;
use crate::data::repositories::archive::chapter_template_repo::ChapterTemplateRepository;
use crate::data::repositories::archive::comic_directory_repo::ComicRepository;
use crate::infra::error::translations::comic_error::ComicError;
use crate::infra::error::translations::db_error::DbError;
use crate::infra::filesystem::files_guard::{
    ArchiveFileGuard, ArtworkFileGuard, FileGuard, ScannerGuard,
};
use crate::infra::filesystem::path_guard::{path_hash, PathGuard};
use crate::infra::filesystem::scanner_engine::{DirectoryEntry, ScannerEngine};
use crate::infra::pattern::chapter_template::detect_template;
use crate::infra::pattern::template_validator::{extract_tags, validate_template};

/// Orquestra o scan de uma biblioteca de quadrinhos no sistema de arquivos.
///
/// Expõe três estratégias de sincronização com o banco de dados:
///
/// - [`refresh_library`]: processa todas as pastas encontradas no disco (upsert bruto).
/// - [`incremental_scan`]: processa apenas pastas novas ou modificadas, remove deletadas.
/// - [`rebuild_library`]: faz o refresh completo e re-escaneia os capítulos de cada comic.
///
/// O progresso é reportado via callback `on_progress: impl FnMut(String)`, permitindo
/// que o command layer emita eventos para o frontend sem acoplamento direto ao `AppHandle`.
///
/// [`refresh_library`]: ComicScannerService::refresh_library
/// [`incremental_scan`]: ComicScannerService::incremental_scan
/// [`rebuild_library`]: ComicScannerService::rebuild_library
pub struct ComicScannerService {
    path_guard: PathGuard,
    comic_repo: ComicRepository,
    chapter_scanner: ChapterScannerService,
    template_repo: ChapterTemplateRepository,
}

impl ComicScannerService {
    pub fn new(root: PathBuf, pool: sqlx::SqlitePool) -> Self {
        Self {
            path_guard: PathGuard::new(root),
            comic_repo: ComicRepository::new(pool.clone()),
            chapter_scanner: ChapterScannerService::new(pool.clone()),
            template_repo: ChapterTemplateRepository::new(pool.clone()),
        }
    }

    /// Processa todas as pastas encontradas no disco, sem comparar com o banco.
    /// Usa INSERT OR IGNORE — se a pasta já existe, pula.
    pub async fn refresh_library(
        &self,
        path: PathBuf,
        mut on_progress: impl FnMut(String),
    ) -> Result<(), ComicError> {
        self.path_guard
            .execute(&path, |_| -> Result<(), String> { Ok(()) })?;

        let templates = self.template_repo.base.find_all().await?;
        let entries = self.collect_entries(path).await?;

        for entry in entries {
            let directory = entry.directory.to_string_lossy().to_string();
            self.process_entry(entry, &templates).await?;
            on_progress(directory);
        }

        Ok(())
    }

    /// Compara o disco com o banco e processa apenas pastas novas ou modificadas.
    /// Remove do banco as pastas que não existem mais no disco.
    pub async fn incremental_scan(
        &self,
        path: PathBuf,
        mut on_progress: impl FnMut(String),
    ) -> Result<(), ComicError> {
        self.path_guard
            .execute(&path, |_| -> Result<(), String> { Ok(()) })?;

        let templates = self.template_repo.base.find_all().await?;
        let discovered = self.collect_entries(path).await?;
        let indexed: Vec<ComicDirectory> = self.comic_repo.base.find_all().await?;

        let indexed_map: HashMap<String, &ComicDirectory> = indexed
            .iter()
            .map(|comic: &ComicDirectory| (comic.path.clone(), comic))
            .collect();

        let discovered_paths: HashSet<String> = discovered
            .iter()
            .map(|entry: &DirectoryEntry| entry.directory.to_string_lossy().to_string())
            .collect();

        // Remove do banco pastas que sumiram do disco
        for comic in &indexed {
            if !discovered_paths.contains(&comic.path) {
                self.comic_repo.base.delete(comic.id).await?;
            }
        }

        for entry in discovered {
            let dir_path = entry.directory.to_string_lossy().to_string();
            let dir_meta = fs::metadata(&entry.directory).await?;
            let disk_modified = modified_secs(&dir_meta);

            let needs_processing = match indexed_map.get(&dir_path) {
                None => true,
                Some(existing) => existing.last_modified < disk_modified,
            };

            if needs_processing {
                on_progress(dir_path);
                self.process_entry(entry, &templates).await?;
            }
        }

        Ok(())
    }

    /// Faz o refresh completo de todas as pastas e re-escaneia os capítulos de cada comic já indexado.
    pub async fn rebuild_library(
        &self,
        path: PathBuf,
        mut on_progress: impl FnMut(String),
    ) -> Result<(), ComicError> {
        self.refresh_library(path, &mut on_progress).await?;

        let all_comics: Vec<ComicDirectory> = self.comic_repo.base.find_all().await?;
        let templates = self.template_repo.base.find_all().await?;

        for comic in all_comics {
            on_progress(comic.path.clone());
            self.rescan_chapters_for(&comic, &templates).await?;
        }

        Ok(())
    }

    /// Dispara o [`ScannerEngine`] e coleta todas as [`DirectoryEntry`] encontradas.
    async fn collect_entries(&self, path: PathBuf) -> Result<Vec<DirectoryEntry>, ComicError> {
        let (tx, mut rx) = mpsc::channel(32);
        let scanner = ScannerEngine::new();
        let _guard = ScannerGuard::new();

        tokio::spawn(async move {
            // FIXME: Colocar tratamento de erros
            scanner.scan(path, tx).await.unwrap();
        });

        let mut entries = Vec::new();
        while let Some(entry) = rx.recv().await {
            entries.push(entry);
        }

        Ok(entries)
    }

    /// Processa uma entrada de diretório: classifica os arquivos, persiste o comic e
    /// delega cada capítulo para [`ChapterScannerService::scan_chapter`].
    ///
    /// Pastas sem arquivos de quadrinhos são silenciosamente ignoradas.
    #[rustfmt::skip]
    async fn process_entry(
        &self,
        entry: DirectoryEntry,
        templates: &[ChapterTemplate],
    ) -> Result<(), ComicError> {
        let archive_guard = ArchiveFileGuard;
        let artwork_guard = ArtworkFileGuard;

        let mut comic_files: Vec<PathBuf> = vec![];
        let mut banner: Option<String> = None;
        let mut cover: Option<String> = None;

        for file in entry.files {
            let name = file.file_name().and_then(|name| name.to_str()).unwrap_or("");

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

        if comic_files.is_empty() {
            return Ok(());
        }

        let detected = self.detect_template_for(&comic_files, templates);
        let template_fk = detected.map(|t| t.id);
        let template_pattern = detected.map(|t| t.pattern.as_str());

        let dir_meta = fs::metadata(&entry.directory).await?;
        let dir_name = entry
            .directory
            .file_name()
            .and_then(|name: &std::ffi::OsStr| name.to_str())
            .unwrap_or("Unknown")
            .to_string();

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
            Err(err) => return Err(err.into()),
        };

        for (index, file) in comic_files.iter().enumerate() {
            self.chapter_scanner.scan_chapter(file, index, saved.id, template_pattern).await?;
        }

        Ok(())
    }

    /// Re-escaneia todos os arquivos de capítulo de um comic já indexado.
    ///
    /// Usado pelo [`rebuild_library`] para garantir que capítulos existentes estejam
    /// atualizados sem duplicar registros (INSERT OR IGNORE).
    ///
    /// [`rebuild_library`]: ComicScannerService::rebuild_library
    #[rustfmt::skip]
    async fn rescan_chapters_for(
        &self,
        comic: &ComicDirectory,
        templates: &[ChapterTemplate],
    ) -> Result<(), ComicError> {
        let comic_path = Path::new(&comic.path);

        if !comic_path.exists() {
            return Ok(());
        }

        let archive_guard = ArchiveFileGuard;
        let mut files: Vec<PathBuf> = vec![];

        let mut read_dir = fs::read_dir(comic_path).await?;
        while let Some(entry) = read_dir.next_entry().await? {
            let path = entry.path();

            if archive_guard.is_allowed(&path).is_ok() {
                files.push(path);
            }
        }

        files.sort();

        let template_pattern = comic
            .chapter_template_fk
            .and_then(|fk| templates.iter().find(|t| t.id == fk))
            .map(|it| it.pattern.as_str());

        for (index, file) in files.iter().enumerate() {
            self.chapter_scanner.scan_chapter(file, index, comic.id, template_pattern).await?;
        }

        Ok(())
    }

    /// Detecta o template de nomenclatura a partir do primeiro arquivo da lista.
    ///
    /// Retorna `None` se nenhum template registrado corresponder ao nome do arquivo.
    fn detect_template_for<'a>(
        &self,
        files: &[PathBuf],
        templates: &'a [ChapterTemplate],
    ) -> Option<&'a ChapterTemplate> {
        files
            .first()
            .and_then(|file| file.file_name())
            .and_then(|name| name.to_str())
            .and_then(|file_str| {
                let template_strs: Vec<&str> =
                    templates.iter().map(|t| t.pattern.as_str()).collect();

                detect_template(file_str, &template_strs, |t| {
                    validate_template(t, extract_tags)
                })
            })
            .and_then(|pattern| templates.iter().find(|t| t.pattern == pattern))
    }
}

/// Retorna o `last_modified` em segundos desde Unix epoch.
/// TODO: Verificar se a forma de ver o last_modified é igual em linux e windows
#[rustfmt::skip]
fn modified_secs(meta: &std::fs::Metadata) -> i64 {
    meta.modified().map(|time| time.duration_since(std::time::UNIX_EPOCH).unwrap_or_default().as_secs() as i64).unwrap_or(0)
}

#[cfg(test)]
mod tests {
    use super::ComicScannerService;
    use crate::data::repositories::archive::chapter_archive_repo::ChapterRepository;
    use crate::data::repositories::archive::comic_directory_repo::ComicRepository;
    use crate::tests::utils::setup_test_db::setup_test_db;
    use std::path::PathBuf;
    use tempfile::TempDir;
    use tokio::fs;

    async fn setup(root: &TempDir) -> (ComicScannerService, sqlx::SqlitePool) {
        let pool = setup_test_db().await;
        let service = ComicScannerService::new(root.path().to_path_buf(), pool.clone());
        (service, pool)
    }

    async fn create_manga_dir(root: &TempDir, name: &str, chapters: &[&str]) -> PathBuf {
        let dir = root.path().join(name);
        fs::create_dir_all(&dir).await.unwrap();
        for chapter in chapters {
            fs::write(dir.join(chapter), b"fake cbz").await.unwrap();
        }
        dir
    }

    async fn count_comics(pool: &sqlx::SqlitePool) -> i64 {
        ComicRepository::new(pool.clone())
            .base
            .count()
            .await
            .unwrap()
    }

    async fn count_chapters(pool: &sqlx::SqlitePool) -> i64 {
        ChapterRepository::new(pool.clone())
            .base
            .count()
            .await
            .unwrap()
    }

    // NOTE: refresh_library

    #[tokio::test]
    async fn refresh_library_indexa_todos_comics() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;

        create_manga_dir(&root, "Berserk", &["Ch. 1.cbz", "Ch. 2.cbz"]).await;
        create_manga_dir(&root, "Vinland Saga", &["Ch. 1.cbz"]).await;

        service
            .refresh_library(root.path().to_path_buf(), |_| {})
            .await
            .unwrap();

        assert_eq!(count_comics(&pool).await, 2);
    }

    #[tokio::test]
    async fn refresh_library_indexa_chapters_de_cada_comic() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;

        create_manga_dir(&root, "Berserk", &["Ch. 1.cbz", "Ch. 2.cbz", "Ch. 3.cbz"]).await;

        service
            .refresh_library(root.path().to_path_buf(), |_| {})
            .await
            .unwrap();

        assert_eq!(count_chapters(&pool).await, 3);
    }

    #[tokio::test]
    async fn refresh_library_ignora_pasta_sem_cbz() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;

        let empty = root.path().join("SemArquivos");
        fs::create_dir_all(&empty).await.unwrap();
        fs::write(empty.join("cover.jpg"), b"img").await.unwrap();

        service
            .refresh_library(root.path().to_path_buf(), |_| {})
            .await
            .unwrap();

        assert_eq!(count_comics(&pool).await, 0);
    }

    // NOTE: incremental_scan

    #[tokio::test]
    async fn incremental_scan_nao_processa_comic_sem_mudanca() {
        let root = tempfile::tempdir().unwrap();
        let (service, _) = setup(&root).await;

        create_manga_dir(&root, "Berserk", &["Ch. 1.cbz"]).await;

        service
            .refresh_library(root.path().to_path_buf(), |_| {})
            .await
            .unwrap();

        let mut progress_count = 0usize;
        service
            .incremental_scan(root.path().to_path_buf(), |_| {
                progress_count += 1;
            })
            .await
            .unwrap();

        assert_eq!(progress_count, 0, "Nenhuma pasta deveria ser reprocessada");
    }

    #[tokio::test]
    async fn incremental_scan_processa_pasta_nova() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;

        create_manga_dir(&root, "Berserk", &["Ch. 1.cbz"]).await;
        service
            .refresh_library(root.path().to_path_buf(), |_| {})
            .await
            .unwrap();

        create_manga_dir(&root, "Vinland Saga", &["Ch. 1.cbz"]).await;

        service
            .incremental_scan(root.path().to_path_buf(), |_| {})
            .await
            .unwrap();

        assert_eq!(count_comics(&pool).await, 2);
    }

    #[tokio::test]
    async fn incremental_scan_remove_pasta_deletada() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;

        create_manga_dir(&root, "Berserk", &["Ch. 1.cbz"]).await;
        create_manga_dir(&root, "Vinland Saga", &["Ch. 1.cbz"]).await;

        service
            .refresh_library(root.path().to_path_buf(), |_| {})
            .await
            .unwrap();

        fs::remove_dir_all(root.path().join("Vinland Saga"))
            .await
            .unwrap();

        service
            .incremental_scan(root.path().to_path_buf(), |_| {})
            .await
            .unwrap();

        let comics = ComicRepository::new(pool.clone())
            .base
            .find_all()
            .await
            .unwrap();
        assert_eq!(comics.len(), 1);
        assert_eq!(comics[0].name, "Berserk");
    }

    // NOTE: rebuild_library

    #[tokio::test]
    async fn rebuild_library_nao_duplica_chapters() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;

        create_manga_dir(&root, "Berserk", &["Ch. 1.cbz", "Ch. 2.cbz"]).await;

        service
            .refresh_library(root.path().to_path_buf(), |_| {})
            .await
            .unwrap();

        let before = count_chapters(&pool).await;

        service
            .rebuild_library(root.path().to_path_buf(), |_| {})
            .await
            .unwrap();

        assert_eq!(count_chapters(&pool).await, before);
    }
}
