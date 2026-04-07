use std::path::{ Path, PathBuf };
use std::hash::{ Hash, Hasher };
use std::collections::hash_map::DefaultHasher;
use tokio::fs;
use tokio::sync::mpsc;

use crate::data::models::archive::comic_directory::ComicDirectory;
use crate::data::models::archive::chapter_archive::ChapterArchive;
use crate::data::repositories::archive::comic_directory_repo::ComicRepository;
use crate::data::repositories::archive::chapter_archive_repo::ChapterRepository;
use crate::infra::filesystem::path_guard::PathGuard;
use crate::infra::filesystem::scanner_engine::ScannerEngine;
use crate::infra::filesystem::files_guard::ScannerGuard;

pub struct ComicScannerService {
    path_guard: PathGuard,
    comic_repo: ComicRepository,
    chapter_repo: ChapterRepository,
}

impl ComicScannerService {
    pub fn new(root: PathBuf, pool: sqlx::SqlitePool) -> Self {
        Self {
            path_guard: PathGuard::new(root),
            comic_repo: ComicRepository::new(pool.clone()),
            chapter_repo: ChapterRepository::new(pool),
        }
    }

    pub async fn scan(&self, path: PathBuf) -> Result<(), String> {
        // NOTE: Valida se o path está dentro do root permitido
        self.path_guard.execute(&path, |_| -> Result<(), String> { Ok(()) })?;

        let (tx, mut rx) = mpsc::channel(32);
        let file_guard = ScannerGuard::new();
        let scanner = ScannerEngine::new();

        tokio::spawn(async move {
            scanner.scan(path, tx).await.unwrap();
        });

        while let Some(entry) = rx.recv().await {
            self.process_entry(entry, &file_guard).await?;
        }

        Ok(())
    }

    async fn process_entry(
        &self,
        entry: crate::infra::filesystem::scanner_engine::DirectoryEntry,
        file_guard: &ScannerGuard
    ) -> Result<(), String> {
        // NOTE: Filtra só arquivos de quadrinhos — descarta o resto
        let comic_files: Vec<PathBuf> = entry.files
            .into_iter()
            .filter(|file| file_guard.is_allowed(file).is_ok())
            .collect();

        if comic_files.is_empty() {
            return Ok(());
        }

        let dir_meta = fs
            ::metadata(&entry.directory).await
            .map_err(|error: std::io::Error| error.to_string())?;

        let dir_name = entry.directory
            .file_name()
            .and_then(|name: &std::ffi::OsStr| name.to_str())
            .unwrap_or("unknown")
            .to_string();

        let dir_last_modified = modified_secs(&dir_meta);

        // TODO: Criar pattern de padrão de nomes de arquivos .cbz .cbr e .pdf
        //       para preencher o chapter_template_fk automaticamente
        // TODO: Fazer o last_modified ter as duas engine de busca rapida e profunda.=
        let comic = ComicDirectory {
            id: path_hash(&entry.directory),
            name: dir_name,
            path: entry.directory.to_string_lossy().to_string(),
            cover: None,
            banner: None,
            last_modified: dir_last_modified,
            chapter_template_fk: None,
            external_sync_enabled: false,
            hidden: false,
        };

        let saved = self.comic_repo.base.insert(&comic).await.map_err(|error| error.to_string())?;

        for file in comic_files {
            self.process_chapter(&file, saved.id).await?;
        }

        Ok(())
    }

    async fn process_chapter(&self, file: &Path, comic_id: i64) -> Result<(), String> {
        let meta = fs::metadata(file).await.map_err(|e| e.to_string())?;

        let file_name = file
            .file_name()
            .and_then(|n| n.to_str())
            .unwrap_or("unknown");

        let file_size = meta.len();
        let file_modified = modified_secs(&meta);

        // NOTE: fast_hash = "filename|size|last_modified"
        let fast_hash = format!("{}|{}|{}", file_name, file_size, file_modified);

        let chapter_name = file
            .file_stem()
            .and_then(|n| n.to_str())
            .unwrap_or("unknown")
            .to_string();

        let chapter = ChapterArchive {
            id: path_hash(file),
            chapter: chapter_name.clone(),
            path: file.to_string_lossy().to_string(),
            chapter_sort: chapter_name,
            fast_hash: Some(fast_hash),
            comic_directory_fk: comic_id,
            last_modified: file_modified,
        };

        self.chapter_repo.base.insert(&chapter).await.map_err(|error| error.to_string())?;

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
fn modified_secs(meta: &std::fs::Metadata) -> i64 {
    meta.modified()
        .map(|t| t.duration_since(std::time::UNIX_EPOCH).unwrap_or_default().as_secs() as i64)
        .unwrap_or(0)
}
