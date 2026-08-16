use std::{
    collections::HashMap,
    path::{Path, PathBuf},
    time::{SystemTime, UNIX_EPOCH},
};

use sqlx::SqlitePool;

use crate::{
    core::services::archive::path_guard::path_hash,
    data::{
        models::archive::{
            chapter_archive::{is_special_name, ChapterArchive},
            comic_directory::ComicDirectory,
        },
        repositories::archive::{
            chapter_archive_repo::ChapterRepository, comic_directory_repo::ComicRepository,
        },
    },
    infra::{
        error::{ComicError, DbError},
        sync::messages::{FileChapterInfo, FileComicInfo, FileManifest},
    },
};

fn now_secs() -> i64 {
    SystemTime::now().duration_since(UNIX_EPOCH).unwrap_or_default().as_secs() as i64
}

/// Substitui caracteres inválidos em nomes de pasta no Windows/Unix por `_`.
fn sanitize_folder_name(name: &str) -> String {
    name.chars()
        .map(|c| if r#"\/:*?"<>|"#.contains(c) { '_' } else { c })
        .collect::<String>()
        .trim()
        .to_string()
}

/// Monta e aplica manifestos de arquivos (os `.cbz`/`.cbr` reais) entre dois devices.
///
/// Diferente do sync de histórico, este PODE criar quadrinhos novos localmente — é o
/// mecanismo que efetivamente "leva a biblioteca" de um device pro outro. Arquivos
/// recebidos são gravados em `<library_root>/synced/<nome sanitizado>/`, nunca dentro da
/// árvore que o usuário organizou manualmente.
#[derive(Clone)]
pub struct FileSyncService {
    comic_repo: ComicRepository,
    chapter_repo: ChapterRepository,
    library_root: PathBuf,
}

impl FileSyncService {
    pub fn new(pool: SqlitePool, library_root: PathBuf) -> Self {
        Self {
            comic_repo: ComicRepository::new(pool.clone()),
            chapter_repo: ChapterRepository::new(pool),
            library_root,
        }
    }

    /// Monta o manifesto local completo, com tamanho de arquivo lido do disco sob demanda
    /// (só roda quando uma sessão de sync de arquivos começa, não a cada scan).
    pub async fn build_manifest(&self) -> Result<FileManifest, ComicError> {
        let rows = self.chapter_repo.find_all_with_comic_name().await?;
        let mut by_comic: HashMap<String, Vec<FileChapterInfo>> = HashMap::new();

        for row in rows {
            let size = tokio::fs::metadata(&row.path).await.map(|meta| meta.len()).unwrap_or(0);
            let file_name = Path::new(&row.path)
                .file_name()
                .and_then(|name| name.to_str())
                .unwrap_or(&row.chapter)
                .to_string();

            by_comic.entry(row.comic_name).or_default().push(FileChapterInfo {
                chapter: row.chapter,
                file_name,
                checksum: row.checksum,
                size,
            });
        }

        let comics = by_comic
            .into_iter()
            .map(|(comic_name, chapters)| FileComicInfo { comic_name, chapters })
            .collect();

        Ok(FileManifest { comics })
    }

    /// Calcula o que EU quero, comparando o manifesto do peer contra a base local: capítulos
    /// ausentes ou com checksum diferente (arquivo desatualizado/corrompido localmente).
    pub async fn diff_wanted(
        &self, peer_manifest: &FileManifest,
    ) -> Result<Vec<(String, String)>, ComicError> {
        let mut wanted = Vec::new();

        for comic in &peer_manifest.comics {
            let local_comic = self.comic_repo.find_by_name(&comic.comic_name).await?;

            for chapter in &comic.chapters {
                let already_have = match &local_comic {
                    Some(existing) => {
                        match self
                            .chapter_repo
                            .find_by_comic_and_chapter(existing.id, &chapter.chapter)
                            .await?
                        {
                            Some(local_chapter) => {
                                local_chapter.checksum.is_some()
                                    && local_chapter.checksum == chapter.checksum
                            },
                            None => false,
                        }
                    },
                    None => false,
                };

                if !already_have {
                    wanted.push((comic.comic_name.clone(), chapter.chapter.clone()));
                }
            }
        }

        Ok(wanted)
    }

    /// Resolve o path local + metadados de um capítulo já existente, pra enviar ao peer.
    pub async fn resolve_local_file(
        &self, comic_name: &str, chapter: &str,
    ) -> Result<Option<(PathBuf, u64, Option<String>, String)>, ComicError> {
        let Some(comic) = self.comic_repo.find_by_name(comic_name).await? else {
            return Ok(None);
        };
        let Some(chapter_row) =
            self.chapter_repo.find_by_comic_and_chapter(comic.id, chapter).await?
        else {
            return Ok(None);
        };

        let path = PathBuf::from(&chapter_row.path);
        let size = tokio::fs::metadata(&path).await.map(|meta| meta.len()).unwrap_or(0);
        let file_name =
            path.file_name().and_then(|name| name.to_str()).unwrap_or(chapter).to_string();

        Ok(Some((path, size, chapter_row.checksum, file_name)))
    }

    /// Move o arquivo já recebido (e verificado) de `temp_path` pro destino final dentro da
    /// biblioteca, criando o quadrinho no banco se ainda não existir, e indexa o capítulo
    /// reaproveitando o mesmo padrão de insert/update do scanner.
    pub async fn persist_received_chapter(
        &self, comic_name: &str, chapter: &str, file_name: &str, temp_path: &Path,
        checksum: String,
    ) -> Result<(), ComicError> {
        let comic = match self.comic_repo.find_by_name(comic_name).await? {
            Some(existing) => existing,
            None => self.create_synced_comic(comic_name).await?,
        };

        let comic_dir = PathBuf::from(&comic.path);
        tokio::fs::create_dir_all(&comic_dir).await.map_err(ComicError::Io)?;

        let dest_path = comic_dir.join(file_name);
        tokio::fs::rename(temp_path, &dest_path).await.map_err(ComicError::Io)?;

        let chapter_row = ChapterArchive {
            id: path_hash(&dest_path),
            chapter: chapter.to_string(),
            path: dest_path.to_string_lossy().to_string(),
            chapter_sort: ChapterArchive::fallback_sort(chapter, 0),
            is_special: is_special_name(chapter),
            checksum: Some(checksum),
            comic_directory_fk: comic.id,
            volume_id_fk: None,
            last_modified: now_secs(),
        };

        match self.chapter_repo.base.insert(&chapter_row).await {
            Ok(_) | Err(DbError::UniqueViolation) => Ok(()),
            Err(error) => Err(ComicError::from(error)),
        }
    }

    /// Cria um novo `comic_directory` sob `<library_root>/synced/<nome>` — usado quando um
    /// capítulo recebido pertence a um quadrinho que ainda não existe localmente.
    async fn create_synced_comic(&self, comic_name: &str) -> Result<ComicDirectory, ComicError> {
        let comic_dir = self.library_root.join("synced").join(sanitize_folder_name(comic_name));

        let comic = ComicDirectory {
            id: path_hash(&comic_dir),
            name: comic_name.to_string(),
            path: comic_dir.to_string_lossy().to_string(),
            cover: None,
            banner: None,
            last_modified: now_secs(),
            archive_template_fk: None,
            external_sync_enabled: true,
            hidden: false,
        };

        match self.comic_repo.base.insert(&comic).await {
            Ok(saved) => Ok(saved),
            Err(DbError::UniqueViolation) => {
                self.comic_repo.find_by_name(comic_name).await?.ok_or(ComicError::NotFound)
            },
            Err(error) => Err(ComicError::from(error)),
        }
    }

    pub fn library_root(&self) -> &Path {
        &self.library_root
    }
}

#[cfg(test)]
mod tests {
    use super::FileSyncService;
    use crate::infra::sync::messages::{FileChapterInfo, FileComicInfo, FileManifest};

    async fn setup() -> (sqlx::SqlitePool, tempfile::TempDir, FileSyncService) {
        let pool = crate::tests::utils::setup_test_db::setup_test_db_with_comic().await;
        sqlx::query("INSERT INTO chapter_archive (id, chapter, path, chapter_sort, is_special, checksum, comic_directory_fk, last_modified) VALUES (1, 'Cap 1', 'p/Cap 1.cbz', '1', 0, 'abc', 1, 0)")
            .execute(&pool)
            .await
            .unwrap();

        let temp_dir = tempfile::tempdir().unwrap();
        let service = FileSyncService::new(pool.clone(), temp_dir.path().to_path_buf());
        (pool, temp_dir, service)
    }

    #[tokio::test]
    async fn diff_wanted_ignora_capitulo_com_mesmo_checksum() {
        let (_, _dir, service) = setup().await;

        let peer_manifest = FileManifest {
            comics: vec![FileComicInfo {
                comic_name: "Test".into(),
                chapters: vec![FileChapterInfo {
                    chapter: "Cap 1".into(),
                    file_name: "Cap 1.cbz".into(),
                    checksum: Some("abc".into()),
                    size: 10,
                }],
            }],
        };

        let wanted = service.diff_wanted(&peer_manifest).await.unwrap();
        assert!(wanted.is_empty());
    }

    #[tokio::test]
    async fn diff_wanted_quer_capitulo_com_checksum_diferente() {
        let (_, _dir, service) = setup().await;

        let peer_manifest = FileManifest {
            comics: vec![FileComicInfo {
                comic_name: "Test".into(),
                chapters: vec![FileChapterInfo {
                    chapter: "Cap 1".into(),
                    file_name: "Cap 1.cbz".into(),
                    checksum: Some("different".into()),
                    size: 10,
                }],
            }],
        };

        let wanted = service.diff_wanted(&peer_manifest).await.unwrap();
        assert_eq!(wanted, vec![("Test".to_string(), "Cap 1".to_string())]);
    }

    #[tokio::test]
    async fn diff_wanted_quer_quadrinho_inexistente_localmente() {
        let (_, _dir, service) = setup().await;

        let peer_manifest = FileManifest {
            comics: vec![FileComicInfo {
                comic_name: "Novo Quadrinho".into(),
                chapters: vec![FileChapterInfo {
                    chapter: "Cap 1".into(),
                    file_name: "Cap 1.cbz".into(),
                    checksum: Some("x".into()),
                    size: 10,
                }],
            }],
        };

        let wanted = service.diff_wanted(&peer_manifest).await.unwrap();
        assert_eq!(wanted, vec![("Novo Quadrinho".to_string(), "Cap 1".to_string())]);
    }

    #[tokio::test]
    async fn persist_received_chapter_cria_quadrinho_novo_e_indexa_capitulo() {
        let (pool, dir, service) = setup().await;

        let temp_path = dir.path().join("incoming.tmp");
        tokio::fs::write(&temp_path, b"fake cbz bytes").await.unwrap();

        service
            .persist_received_chapter(
                "Quadrinho Recebido",
                "Cap 1",
                "Cap 1.cbz",
                &temp_path,
                "checksum123".to_string(),
            )
            .await
            .unwrap();

        let comic: (i64,) =
            sqlx::query_as("SELECT COUNT(*) FROM comic_directory WHERE name = 'Quadrinho Recebido'")
                .fetch_one(&pool)
                .await
                .unwrap();
        assert_eq!(comic.0, 1);

        let chapter: (i64,) =
            sqlx::query_as("SELECT COUNT(*) FROM chapter_archive WHERE chapter = 'Cap 1' AND checksum = 'checksum123'")
                .fetch_one(&pool)
                .await
                .unwrap();
        assert_eq!(chapter.0, 1);

        assert!(!temp_path.exists(), "temp file should have been moved, not copied");
    }
}
