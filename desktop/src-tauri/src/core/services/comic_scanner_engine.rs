use std::collections::hash_map::DefaultHasher;
use std::hash::{Hash, Hasher};
use std::path::{Path, PathBuf};
use tauri::{AppHandle, Emitter};
use tokio::fs;
use tokio::sync::mpsc;

use crate::data::models::archive::chapter_archive::ChapterArchive;
use crate::data::models::archive::comic_directory::ComicDirectory;
use crate::data::repositories::archive::chapter_archive_repo::ChapterRepository;
use crate::data::repositories::archive::comic_directory_repo::ComicRepository;
use crate::infra::error::translations::comic_error::ComicError;
use crate::infra::filesystem::files_guard::{ArchiveFileGuard, ScannerGuard};
use crate::infra::filesystem::files_guard::{ArtworkFileGuard, FileGuard};
use crate::infra::filesystem::path_guard::PathGuard;
use crate::infra::filesystem::scanner_engine::ScannerEngine;

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

    pub async fn scan(&self, path: PathBuf, app: &AppHandle) -> Result<(), ComicError> {
        // INFO: Valida se o path está dentro do root permitido
        // FIXME: Colocar tratamento de erros
        self.path_guard
            .execute(&path, |_| -> Result<(), String> { Ok(()) })
            .map_err(|error: String| ComicError::SystemFailure(error))?;

        let (tx, mut rx) = mpsc::channel(32);
        let file_guard = ScannerGuard::new();
        let scanner = ScannerEngine::new();

        tokio::spawn(async move {
            // FIXME: Colocar tratamento de erros
            scanner.scan(path, tx).await.unwrap();
        });

        while let Some(entry) = rx.recv().await {
            let directory = entry.directory.to_string_lossy().to_string();

            // Emite o progresso e qual pasta está sendo escaneada
            self.process_entry(entry, &file_guard).await?;
            let _ = app.emit("scan:progress", directory);
        }

        Ok(())
    }

    async fn process_entry(
        &self,
        entry: crate::infra::filesystem::scanner_engine::DirectoryEntry,
        _file_guard: &ScannerGuard,
    ) -> Result<(), ComicError> {
        // FIXME: Colocar tratamento de erros
        let archive_guard = ArchiveFileGuard;
        // FIXME: Colocar tratamento de erros
        let artwork_guard = ArtworkFileGuard;

        let mut comic_files: Vec<PathBuf> = vec![];
        let mut cover: Option<String> = None;
        let mut banner: Option<String> = None;

        for file in entry.files {
            let name = file.file_name().and_then(|n| n.to_str()).unwrap_or("");

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

            // TODO: ComicInfo.xml, .pdf e outros ignorados por ora
        }

        // NOTE: Só persiste se tiver arquivos de quadrinhos
        if comic_files.is_empty() {
            return Ok(());
        }

        let dir_meta = fs::metadata(&entry.directory).await?;

        let dir_name = entry
            .directory
            .file_name()
            .and_then(|name: &std::ffi::OsStr| name.to_str())
            .unwrap_or("unknown")
            .to_string();

        // TODO: Criar pattern de padrão de nomes de arquivos .cbz .cbr e .pdf
        //       para preencher o chapter_template_fk automaticamente
        // TODO: Fazer o last_modified abrir porta para o Deep sync e fast sync
        let comic = ComicDirectory {
            id: path_hash(&entry.directory),
            name: dir_name,
            path: entry.directory.to_string_lossy().to_string(),
            cover,
            banner,
            last_modified: modified_secs(&dir_meta),
            chapter_template_fk: None,
            external_sync_enabled: false,
            hidden: false,
        };

        let saved = self.comic_repo.base.insert(&comic).await?;

        for file in comic_files {
            self.process_chapter(&file, saved.id).await?;
        }

        Ok(())
    }

    async fn process_chapter(&self, file: &Path, comic_id: i64) -> Result<(), ComicError> {
        let meta = fs::metadata(file).await?;

        let file_name = file
            .file_name()
            .and_then(|it| it.to_str())
            .ok_or_else(|| ComicError::SystemFailure("Nome de arquivo inválido".into()))?;

        let file_size = meta.len();
        let file_modified = modified_secs(&meta);

        let fast_hash = format!("{}|{}|{}", file_name, file_size, file_modified);

        let chapter_name = file
            .file_stem()
            .and_then(|it| it.to_str())
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

        self.chapter_repo.base.insert(&chapter).await?;
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
fn modified_secs(meta: &std::fs::Metadata) -> i64 {
    // FIXME: Colocar tratamento de erros
    meta.modified()
        .map(|time: std::time::SystemTime| {
            time.duration_since(std::time::UNIX_EPOCH)
                .unwrap_or_default()
                .as_secs() as i64
        })
        .unwrap_or(0)
}
