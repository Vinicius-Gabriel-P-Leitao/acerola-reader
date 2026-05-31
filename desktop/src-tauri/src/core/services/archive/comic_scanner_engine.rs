use std::collections::{HashMap, HashSet};
use std::future::Future;
use std::path::PathBuf;
use tokio::fs;
use tokio::sync::mpsc;

use crate::core::services::archive::chapter_scanner_engine::ChapterScannerService;
use crate::core::services::archive::files_guard::{
    ArchiveFileGuard, ArtworkFileGuard, FileGuard, ScannerGuard,
};
use crate::core::services::archive::path_guard::{path_hash, PathGuard};
use crate::data::models::archive::archive_template::{ArchiveTemplate, SortType};
use crate::data::models::archive::chapter_archive::ChapterArchive;
use crate::data::models::archive::comic_directory::ComicDirectory;
use crate::data::models::archive::volume_archive::VolumeArchive;
use crate::data::repositories::archive::archive_template_repo::ArchiveTemplateRepository;
use crate::data::repositories::archive::comic_directory_repo::ComicRepository;
use crate::data::repositories::archive::volume_archive_repo::VolumeRepository;
use crate::infra::error::ComicError;
use crate::infra::error::DbError;
use crate::infra::filesystem::{DirectoryEntry, ScannerEngine};
use crate::infra::pattern::template::{detect_template, extract_chapter_parts};
use crate::infra::pattern::template_validator::validate_chapter_template;

/// Orquestra o scan de uma biblioteca de quadrinhos no sistema de arquivos.
///
/// ### Identificação e IDs
/// Este serviço utiliza IDs determinísticos baseados em [`path_hash`]. Isso permite que
/// o scanner identifique quadrinhos e capítulos de forma estável sem depender do estado
/// interno do banco de dados, facilitando scans incrementais rápidos e futura sincronização P2P.
///
/// ### Regras de Escaneamento
/// - **Recursividade:** O scanner explora a árvore de diretórios sem limite de profundidade.
/// - **Critério de Comic:** Uma pasta só é considerada um quadrinho se contiver arquivos de arquivo
///   (ex: .cbz) ou subpastas que correspondam a templates de Volume.
/// - **Topmost Comic:** Uma vez que uma pasta é identificada como quadrinho, suas subpastas
///   são processadas apenas como volumes internos, evitando duplicatas na biblioteca.
///
/// Expõe três estratégias de sincronização com o banco de dados:
pub struct ComicScannerService {
    path_guard: PathGuard,
    comic_repo: ComicRepository,
    chapter_scanner: ChapterScannerService,
    template_repo: ArchiveTemplateRepository,
    volume_repo: VolumeRepository,
}

impl ComicScannerService {
    pub fn new(root: PathBuf, pool: sqlx::SqlitePool) -> Self {
        Self {
            path_guard: PathGuard::new(root),
            comic_repo: ComicRepository::new(pool.clone()),
            chapter_scanner: ChapterScannerService::new(pool.clone()),
            template_repo: ArchiveTemplateRepository::new(pool.clone()),
            volume_repo: VolumeRepository::new(pool.clone()),
        }
    }

    /// Processa todas as pastas encontradas no disco.
    /// INSERT OR IGNORE — pastas já indexadas são ignoradas.
    pub async fn refresh_library(
        &self, path: PathBuf, mut on_progress: impl FnMut(String),
    ) -> Result<(), ComicError> {
        self.path_guard.execute(&path, |_| -> Result<(), String> { Ok(()) })?;

        let templates = self.template_repo.base.find_all().await?;
        let mut entries = self.collect_entries(path).await?;
        entries.sort_by_key(|e| e.directory.clone());

        let repo = self.comic_repo.clone();
        let mut processed_paths: Vec<String> = vec![];

        for entry in entries {
            let directory = entry.directory.to_string_lossy().to_string();

            // Pula se o diretório atual for subdiretório de algum já processado como comic
            if processed_paths.iter().any(|it| directory.starts_with(it) && directory != *it) {
                continue;
            }

            let r = repo.clone();
            let is_comic = self
                .process_entry(entry, &templates, |comic| async move {
                    match r.base.insert(&comic).await {
                        Ok(saved) => Ok(saved),
                        Err(DbError::UniqueViolation) => Ok(comic),
                        Err(err) => Err(ComicError::from(err)),
                    }
                })
                .await?;

            if is_comic {
                on_progress(directory.clone());
                processed_paths.push(directory);
            }
        }

        Ok(())
    }

    /// Compara o disco com o banco e processa apenas pastas novas ou modificadas (upsert).
    /// Remove do banco as pastas que não existem mais no disco.
    pub async fn incremental_scan(
        &self, path: PathBuf, mut on_progress: impl FnMut(String),
    ) -> Result<(), ComicError> {
        self.path_guard.execute(&path, |_| -> Result<(), String> { Ok(()) })?;

        let templates = self.template_repo.base.find_all().await?;
        let mut discovered = self.collect_entries(path).await?;
        discovered.sort_by_key(|e| e.directory.clone());

        let indexed: Vec<ComicDirectory> = self.comic_repo.base.find_all().await?;
        let repo = self.comic_repo.clone();

        let indexed_map: HashMap<String, &ComicDirectory> =
            indexed.iter().map(|comic: &ComicDirectory| (comic.path.clone(), comic)).collect();

        let discovered_paths: HashSet<String> = discovered
            .iter()
            .map(|entry: &DirectoryEntry| entry.directory.to_string_lossy().to_string())
            .collect();

        for comic in &indexed {
            if !discovered_paths.contains(&comic.path) {
                self.comic_repo.base.delete(comic.id).await?;
            }
        }

        let mut processed_paths: Vec<String> = vec![];

        for entry in discovered {
            let dir_path = entry.directory.to_string_lossy().to_string();

            // Pula se o diretório atual for subdiretório de algum já processado como comic
            if processed_paths.iter().any(|p| dir_path.starts_with(p) && dir_path != *p) {
                continue;
            }

            let dir_meta = fs::metadata(&entry.directory).await?;
            let disk_modified = modified_secs(&dir_meta);

            let existing = indexed_map.get(&dir_path);
            let needs_processing = match existing {
                None => true,
                Some(e) => e.last_modified < disk_modified,
            };

            let is_comic = if needs_processing {
                let repository = repo.clone();
                let was_processed = self
                    .process_entry(entry, &templates, |comic| async move {
                        match repository.base.insert(&comic).await {
                            Ok(saved) => Ok(saved),
                            Err(DbError::UniqueViolation) => {
                                repository.base.update(&comic).await.map_err(ComicError::from)
                            },
                            Err(err) => Err(ComicError::from(err)),
                        }
                    })
                    .await?;

                if was_processed {
                    on_progress(dir_path.clone());
                }
                was_processed
            } else {
                // Se já existe e não mudou, ele É um comic
                true
            };

            if is_comic {
                processed_paths.push(dir_path);
            }
        }

        Ok(())
    }

    /// Sobrescreve todos os comics encontrados no disco, ignorando o estado atual do banco.
    /// DELETE + INSERT — capítulos são removidos via CASCADE e reinseridos.
    pub async fn rebuild_library(
        &self, path: PathBuf, mut on_progress: impl FnMut(String),
    ) -> Result<(), ComicError> {
        self.path_guard.execute(&path, |_| -> Result<(), String> { Ok(()) })?;

        let templates = self.template_repo.base.find_all().await?;
        let mut entries = self.collect_entries(path).await?;
        entries.sort_by_key(|entry| entry.directory.clone());

        let repo = self.comic_repo.clone();
        let mut processed_paths: Vec<String> = vec![];

        for entry in entries {
            let directory = entry.directory.to_string_lossy().to_string();

            // Pula se o diretório atual for subdiretório de algum já processado como comic
            if processed_paths.iter().any(|p| directory.starts_with(p) && directory != *p) {
                continue;
            }

            let repo = repo.clone();
            let is_comic = self
                .process_entry(entry, &templates, |comic| async move {
                    repo.base.delete(comic.id).await?;
                    repo.base.insert(&comic).await.map_err(ComicError::from)
                })
                .await?;

            if is_comic {
                on_progress(directory.clone());
                processed_paths.push(directory);
            }
        }

        Ok(())
    }

    /// Limita a profundidade a 1 nível (filhos diretos do root = comic directories).
    /// Subdiretórios de comics são expostos via `DirectoryEntry.subdirs` para detecção de volumes.
    async fn collect_entries(&self, path: PathBuf) -> Result<Vec<DirectoryEntry>, ComicError> {
        let (tx, mut rx) = mpsc::channel(32);
        let scanner = ScannerEngine { max_depth: None };
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

    /// Classifica os arquivos de um diretório, monta o [`ComicDirectory`] e delega a
    /// persistência para `persist`. Após persistir, escaneia capítulos raiz e volumes.
    ///
    /// `persist` recebe o comic montado e decide a estratégia de escrita no banco:
    /// - `refresh_library` injeta INSERT OR IGNORE
    /// - `incremental_scan` injeta upsert (INSERT ou UPDATE)
    /// - `rebuild_library` injeta DELETE + INSERT
    async fn process_entry<F, Fut>(
        &self, entry: DirectoryEntry, templates: &[ArchiveTemplate], persist: F,
    ) -> Result<bool, ComicError>
    where
        F: FnOnce(ComicDirectory) -> Fut,
        Fut: Future<Output = Result<ComicDirectory, ComicError>>,
    {
        let archive_guard = ArchiveFileGuard;
        let artwork_guard = ArtworkFileGuard;

        let chapter_templates: Vec<&ArchiveTemplate> =
            templates.iter().filter(|template| template.sort_type == SortType::Chapter).collect();

        let volume_templates: Vec<&ArchiveTemplate> =
            templates.iter().filter(|template| template.sort_type == SortType::Volume).collect();

        let mut comic_cover = None;
        let mut comic_banner = None;
        let mut comic_files = Vec::new();

        for file in entry.files {
            let is_cover = artwork_guard.is_cover(&file);
            let is_banner = artwork_guard.is_banner(&file);
            let is_archive = archive_guard.is_allowed(&file).is_ok();

            if comic_cover.is_none() && is_cover {
                comic_cover = Some(file.to_string_lossy().to_string());
            }

            if comic_banner.is_none() && is_banner {
                comic_banner = Some(file.to_string_lossy().to_string());
            }

            if is_archive {
                comic_files.push(file);
            }
        }

        // Subdiretórios candidatos a volume
        let volume_pattern_strs: Vec<&str> = volume_templates
            .iter()
            .map(|template: &&ArchiveTemplate| template.pattern.as_str())
            .collect();

        let mut matched_volumes = vec![];
        for subdir in &entry.subdirs {
            let dir_name = subdir.file_name().and_then(|name| name.to_str()).unwrap_or("");
            if let Some(pattern) = detect_template(dir_name, &volume_pattern_strs, |_| Ok(())) {
                matched_volumes.push((subdir, pattern));
            }
        }

        // Ignora o diretório se não tiver arquivos de quadrinho nem volumes candidatos
        if comic_files.is_empty() && matched_volumes.is_empty() {
            return Ok(false);
        }

        let detected = self.detect_template_for(&comic_files, &chapter_templates);
        let template_fk = detected.map(|template| template.id);
        let template_pattern = detected.map(|template| template.pattern.as_str());

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
            cover: comic_cover,
            banner: comic_banner,
            last_modified: modified_secs(&dir_meta),
            archive_template_fk: template_fk,
            external_sync_enabled: false,
            hidden: false,
        };

        let saved = persist(comic).await?;

        // Capítulos raiz (direto no diretório do comic, sem volume)
        for (index, file) in comic_files.iter().enumerate() {
            self.chapter_scanner
                .scan_chapter(file, index, saved.id, None, template_pattern)
                .await?;
        }

        for (subdir, pattern) in matched_volumes {
            let dir_name = subdir.file_name().and_then(|name| name.to_str()).unwrap_or("");

            let volume_sort = extract_chapter_parts(dir_name, pattern, |_| Ok(()))
                .map(|(vol, dec)| ChapterArchive::format_sort(vol, dec))
                .unwrap_or_else(|| dir_name.to_string());

            let subdir_meta = fs::metadata(subdir).await?;
            let is_special =
                crate::data::models::archive::chapter_archive::is_special_name(dir_name);

            let volume_files = self.collect_files(subdir).await.unwrap_or_default();

            let mut vol_cover = None;
            let mut vol_banner = None;
            let mut vol_archives = Vec::new();

            for file in volume_files {
                if vol_cover.is_none() && artwork_guard.is_cover(&file) {
                    vol_cover = Some(file.to_string_lossy().to_string());
                    continue;
                }

                if vol_banner.is_none() && artwork_guard.is_banner(&file) {
                    vol_banner = Some(file.to_string_lossy().to_string());
                    continue;
                }

                if archive_guard.is_allowed(&file).is_ok() {
                    vol_archives.push(file);
                }
            }

            let volume_id = path_hash(subdir);
            let volume = VolumeArchive {
                id: volume_id,
                name: dir_name.to_string(),
                path: subdir.to_string_lossy().to_string(),
                volume_sort,
                is_special,
                cover: vol_cover,
                banner: vol_banner,
                comic_directory_fk: saved.id,
                last_modified: modified_secs(&subdir_meta),
            };

            match self.volume_repo.base.insert(&volume).await {
                Ok(_) | Err(DbError::UniqueViolation) => {},
                Err(err) => return Err(ComicError::from(err)),
            }

            vol_archives.sort();

            for (index, file) in vol_archives.iter().enumerate() {
                self.chapter_scanner
                    .scan_chapter(file, index, saved.id, Some(volume_id), template_pattern)
                    .await?;
            }
        }

        Ok(true)
    }

    /// Coleta todos os arquivos dentro de um diretório.
    async fn collect_files(&self, dir: &PathBuf) -> Result<Vec<PathBuf>, ComicError> {
        let mut entries = fs::read_dir(dir).await?;
        let mut files = vec![];

        while let Ok(Some(entry)) = entries.next_entry().await {
            let path = entry.path();
            if entry.metadata().await.map(|meta| meta.is_file()).unwrap_or(false) {
                files.push(path);
            }
        }

        Ok(files)
    }

    fn detect_template_for<'a>(
        &self, files: &[PathBuf], templates: &[&'a ArchiveTemplate],
    ) -> Option<&'a ArchiveTemplate> {
        files
            .first()
            .and_then(|file| file.file_name())
            .and_then(|name| name.to_str())
            .and_then(|file_str| {
                let template_strs: Vec<&str> =
                    templates.iter().map(|template| template.pattern.as_str()).collect();

                detect_template(file_str, &template_strs, validate_chapter_template)
            })
            .and_then(|pattern| {
                templates.iter().copied().find(|template| template.pattern == pattern)
            })
    }
}

#[rustfmt::skip]
fn modified_secs(meta: &std::fs::Metadata) -> i64 {
    meta.modified().map(|time| time.duration_since(std::time::UNIX_EPOCH).unwrap_or_default().as_secs() as i64).unwrap_or(0)
}

#[cfg(test)]
mod tests {
    use super::ComicScannerService;
    use crate::data::repositories::archive::chapter_archive_repo::ChapterRepository;
    use crate::data::repositories::archive::comic_directory_repo::ComicRepository;
    use crate::data::repositories::archive::volume_archive_repo::VolumeRepository;
    use crate::tests::utils::setup_test_db::{reset_comics_last_modified, setup_test_db};
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

    async fn create_volume_dir(
        root: &TempDir, comic: &str, volume: &str, chapters: &[&str],
    ) -> PathBuf {
        let dir = root.path().join(comic).join(volume);
        fs::create_dir_all(&dir).await.unwrap();
        for chapter in chapters {
            fs::write(dir.join(chapter), b"fake cbz").await.unwrap();
        }
        dir
    }

    async fn count_comics(pool: &sqlx::SqlitePool) -> i64 {
        ComicRepository::new(pool.clone()).base.count().await.unwrap()
    }

    async fn count_chapters(pool: &sqlx::SqlitePool) -> i64 {
        ChapterRepository::new(pool.clone()).base.count().await.unwrap()
    }

    async fn count_volumes(pool: &sqlx::SqlitePool) -> i64 {
        VolumeRepository::new(pool.clone()).base.count().await.unwrap()
    }

    #[tokio::test]
    async fn refresh_library_indexa_todos_comics() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;

        create_manga_dir(&root, "Berserk", &["Ch. 1.cbz", "Ch. 2.cbz"]).await;
        create_manga_dir(&root, "Vinland Saga", &["Ch. 1.cbz"]).await;

        service.refresh_library(root.path().to_path_buf(), |_| {}).await.unwrap();
        assert_eq!(count_comics(&pool).await, 2);
    }

    #[tokio::test]
    async fn refresh_library_indexa_chapters_de_cada_comic() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;

        create_manga_dir(&root, "Berserk", &["Ch. 1.cbz", "Ch. 2.cbz", "Ch. 3.cbz"]).await;
        service.refresh_library(root.path().to_path_buf(), |_| {}).await.unwrap();
        assert_eq!(count_chapters(&pool).await, 3);
    }

    #[tokio::test]
    async fn refresh_library_ignora_pasta_sem_cbz() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;
        let empty = root.path().join("SemArquivos");
        fs::create_dir_all(&empty).await.unwrap();
        fs::write(empty.join("cover.jpg"), b"img").await.unwrap();
        service.refresh_library(root.path().to_path_buf(), |_| {}).await.unwrap();
        assert_eq!(count_comics(&pool).await, 0);
    }

    #[tokio::test]
    async fn refresh_library_nao_duplica_ao_rodar_duas_vezes() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;
        create_manga_dir(&root, "Berserk", &["Ch. 1.cbz", "Ch. 2.cbz"]).await;
        service.refresh_library(root.path().to_path_buf(), |_| {}).await.unwrap();
        service.refresh_library(root.path().to_path_buf(), |_| {}).await.unwrap();
        assert_eq!(count_comics(&pool).await, 1);
        assert_eq!(count_chapters(&pool).await, 2);
    }

    #[tokio::test]
    async fn refresh_library_indexa_volumes() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;
        create_volume_dir(&root, "Berserk", "Vol. 01", &["Ch. 1.cbz", "Ch. 2.cbz"]).await;
        create_volume_dir(&root, "Berserk", "Vol. 02", &["Ch. 3.cbz"]).await;

        // Seed de templates de volume
        // FIXME: Trocar isso por uma função que existe em tests/
        let pool_ref = &pool;
        sqlx::query(include_str!("../../../../migrations/seeds/001_seed_chapter_template.sql"))
            .execute(pool_ref)
            .await
            .unwrap();

        service.refresh_library(root.path().to_path_buf(), |_| {}).await.unwrap();

        assert_eq!(count_comics(&pool).await, 1);
        assert_eq!(count_volumes(&pool).await, 2);
        assert_eq!(count_chapters(&pool).await, 3);
    }

    #[tokio::test]
    async fn refresh_library_capitulos_raiz_sem_volume_id() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;

        create_manga_dir(&root, "Berserk", &["Ch. 1.cbz"]).await;
        service.refresh_library(root.path().to_path_buf(), |_| {}).await.unwrap();

        let chapters = ChapterRepository::new(pool.clone()).base.find_all().await.unwrap();
        assert!(chapters[0].volume_id_fk.is_none());
    }

    #[tokio::test]
    async fn refresh_library_recursivo_com_root_e_quadrinhos_aninhados() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;

        // Estrutura:
        // root/
        //   Mangas/ (não deve ser comic)
        //     Berserk/ (deve ser comic)
        //       Vol. 01/ (deve ser volume de Berserk, não comic)
        //         Ch 1.cbz
        //   Hq/ (deve ser comic)
        //     Spiderman.cbz (capítulo de Hq)

        let mangas_dir = root.path().join("Mangas");
        let berserk_dir = mangas_dir.join("Berserk");
        let vol1_dir = berserk_dir.join("Vol. 01");
        fs::create_dir_all(&vol1_dir).await.unwrap();
        fs::write(vol1_dir.join("Ch 1.cbz"), b"fake").await.unwrap();

        let hq_dir = root.path().join("Hq");
        fs::create_dir_all(&hq_dir).await.unwrap();
        fs::write(hq_dir.join("Spiderman.cbz"), b"fake").await.unwrap();

        // Seed de templates de volume para Berserk funcionar
        let pool_ref = &pool;
        sqlx::query(include_str!("../../../../migrations/seeds/001_seed_chapter_template.sql"))
            .execute(pool_ref)
            .await
            .unwrap();

        service.refresh_library(root.path().to_path_buf(), |_| {}).await.unwrap();

        let comics = ComicRepository::new(pool.clone()).base.find_all().await.unwrap();
        let names: Vec<String> = comics.iter().map(|c| c.name.clone()).collect();

        // Mangas não deve estar aqui porque só tem subpastas que não são volumes
        // Hq deve estar porque tem Spiderman.cbz
        // Berserk deve estar porque tem subpastas que são volumes
        assert!(names.contains(&"Berserk".to_string()));
        assert!(names.contains(&"Hq".to_string()));
        assert!(!names.contains(&"Mangas".to_string()));
        assert!(!names.contains(&"Vol. 01".to_string()));

        assert_eq!(count_comics(&pool).await, 2);
        assert_eq!(count_volumes(&pool).await, 1, "Berserk deveria ter 1 volume");
    }

    #[tokio::test]
    async fn incremental_scan_nao_processa_comic_sem_mudanca() {
        let root = tempfile::tempdir().unwrap();
        let (service, _) = setup(&root).await;
        create_manga_dir(&root, "Berserk", &["Ch. 1.cbz"]).await;
        service.refresh_library(root.path().to_path_buf(), |_| {}).await.unwrap();

        // Resetamos o estado de modificação para garantir que o scan incremental veja como "não mudou"
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

        service.refresh_library(root.path().to_path_buf(), |_| {}).await.unwrap();
        create_manga_dir(&root, "Vinland Saga", &["Ch. 1.cbz"]).await;

        service.incremental_scan(root.path().to_path_buf(), |_| {}).await.unwrap();
        assert_eq!(count_comics(&pool).await, 2);
    }

    #[tokio::test]
    async fn incremental_scan_remove_pasta_deletada() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;

        create_manga_dir(&root, "Berserk", &["Ch. 1.cbz"]).await;
        create_manga_dir(&root, "Vinland Saga", &["Ch. 1.cbz"]).await;

        service.refresh_library(root.path().to_path_buf(), |_| {}).await.unwrap();
        fs::remove_dir_all(root.path().join("Vinland Saga")).await.unwrap();

        service.incremental_scan(root.path().to_path_buf(), |_| {}).await.unwrap();
        let comics = ComicRepository::new(pool.clone()).base.find_all().await.unwrap();
        assert_eq!(comics.len(), 1);
        assert_eq!(comics[0].name, "Berserk");
    }

    #[tokio::test]
    async fn incremental_scan_atualiza_cover_em_pasta_modificada() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;
        let dir = create_manga_dir(&root, "Berserk", &["Ch. 1.cbz"]).await;

        service.refresh_library(root.path().to_path_buf(), |_| {}).await.unwrap();
        let before = ComicRepository::new(pool.clone()).base.find_all().await.unwrap();

        assert!(before[0].cover.is_none());
        reset_comics_last_modified(&pool).await;
        fs::write(dir.join("cover.jpg"), b"fake cover").await.unwrap();

        service.incremental_scan(root.path().to_path_buf(), |_| {}).await.unwrap();
        let after = ComicRepository::new(pool.clone()).base.find_all().await.unwrap();
        assert!(after[0].cover.is_some(), "cover deveria ter sido atualizado pelo incremental");
    }

    #[tokio::test]
    async fn rebuild_library_nao_duplica_chapters() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;

        create_manga_dir(&root, "Berserk", &["Ch. 1.cbz", "Ch. 2.cbz"]).await;
        service.refresh_library(root.path().to_path_buf(), |_| {}).await.unwrap();

        let before = count_chapters(&pool).await;
        service.rebuild_library(root.path().to_path_buf(), |_| {}).await.unwrap();
        assert_eq!(count_chapters(&pool).await, before);
    }

    #[tokio::test]
    async fn refresh_library_indexa_volumes_especiais() {
        let root = tempfile::tempdir().unwrap();
        let (service, pool) = setup(&root).await;
        create_volume_dir(&root, "Berserk", "Vol. special", &["Ch. 0.01.cbz"]).await;

        // Seed de templates de volume
        let pool_ref = &pool;
        sqlx::query(include_str!("../../../../migrations/seeds/001_seed_chapter_template.sql"))
            .execute(pool_ref)
            .await
            .unwrap();

        service.refresh_library(root.path().to_path_buf(), |_| {}).await.unwrap();

        assert_eq!(count_comics(&pool).await, 1);
        assert_eq!(count_volumes(&pool).await, 1, "Deveria ter indexado o volume 'Vol. special'");
        assert_eq!(count_chapters(&pool).await, 1);
    }
}
