use std::path::Path;

use tokio::fs;

use crate::{
    core::services::archive::path_guard::path_hash,
    data::{
        models::archive::chapter_archive::{is_special_name, ChapterArchive},
        repositories::archive::chapter_archive_repo::ChapterRepository,
    },
    infra::{
        error::{ComicError, DbError},
        pattern::{template::extract_chapter_parts, template_validator::validate_chapter_template},
    },
};

/// Responsável por indexar capítulos individuais no banco de dados.
///
/// Recebe um arquivo de capítulo, extrai seus metadados e o persiste.
/// Chamado pelo [`super::comic_scanner_engine::ComicScannerService`] durante qualquer
/// forma de scan de biblioteca.
pub struct ChapterScannerService {
    chapter_repo: ChapterRepository,
}

impl ChapterScannerService {
    pub fn new(pool: sqlx::SqlitePool) -> Self {
        Self { chapter_repo: ChapterRepository::new(pool) }
    }

    /// Remove todos os capítulos indexados de um quadrinho. Usado pelo rescan profundo
    /// para invalidar o estado atual antes de re-escanear a pasta do zero.
    pub async fn delete_by_comic(&self, comic_id: i64) -> Result<(), ComicError> {
        self.chapter_repo.delete_by_comic(comic_id).await?;
        Ok(())
    }

    /// Indexa um único arquivo de capítulo no banco de dados.
    ///
    /// Extrai nome, hash rápido (`nome|tamanho|modificado`) e `chapter_sort` a partir do
    /// template detectado. Se nenhum template for fornecido, usa [`ChapterArchive::fallback_sort`].
    ///
    /// Duplicatas são silenciosamente ignoradas via `UNIQUE` constraint — o mesmo arquivo
    /// pode ser escaneado múltiplas vezes sem criar registros duplicados.
    #[rustfmt::skip]
    pub async fn scan_chapter(
        &self,
        file: &Path,
        index: usize,
        comic_id: i64,
        volume_id: Option<i64>,
        template: Option<&str>,
    ) -> Result<(), ComicError> {
        let meta = fs::metadata(file).await?;

        let file_name = file
            .file_name()
            .and_then(|it| it.to_str())
            .ok_or_else(|| ComicError::SystemFailure("File name is invalid".into()))?;

        let file_modified = modified_secs(&meta);

        let chapter_name = file.file_stem().and_then(|it| it.to_str()).unwrap_or("unknown").to_string();

        let chapter_sort = template
            .and_then(|template| {
                extract_chapter_parts(file_name, template, validate_chapter_template)
            })
            .map(|(chapter, decimal)| ChapterArchive::format_sort(chapter, decimal))
            .unwrap_or_else(|| ChapterArchive::fallback_sort(&chapter_name, index));

        let chapter = ChapterArchive {
            id: path_hash(file),
            chapter: chapter_name.clone(),
            path: file.to_string_lossy().to_string(),
            chapter_sort,
            is_special: is_special_name(&chapter_name),
            checksum: None,
            comic_directory_fk: comic_id,
            volume_id_fk: volume_id,
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
            Err(err) => return Err(err.into()),
        }

        Ok(())
    }
}

#[rustfmt::skip]
fn modified_secs(meta: &std::fs::Metadata) -> i64 {
    meta.modified().map(|time| time.duration_since(std::time::UNIX_EPOCH).unwrap_or_default().as_secs() as i64).unwrap_or(0)
}

#[cfg(test)]
mod tests {
    use std::path::PathBuf;

    use tempfile::TempDir;
    use tokio::fs;

    use super::ChapterScannerService;
    use crate::{
        data::repositories::archive::chapter_archive_repo::ChapterRepository,
        tests::utils::setup_test_db::setup_test_db_with_comic,
    };

    async fn setup() -> (ChapterScannerService, sqlx::SqlitePool, TempDir) {
        let pool = setup_test_db_with_comic().await;
        let service = ChapterScannerService::new(pool.clone());
        let dir = tempfile::tempdir().unwrap();
        (service, pool, dir)
    }

    async fn create_file(dir: &TempDir, name: &str) -> PathBuf {
        let path = dir.path().join(name);
        fs::write(&path, b"fake cbz content").await.unwrap();
        path
    }

    fn chapter_repo(pool: &sqlx::SqlitePool) -> ChapterRepository {
        ChapterRepository::new(pool.clone())
    }

    #[tokio::test]
    async fn scan_chapter_insere_no_banco() {
        let (service, pool, dir) = setup().await;
        let file = create_file(&dir, "Ch. 1.cbz").await;
        service.scan_chapter(&file, 0, 1, None, None).await.unwrap();
        let all = chapter_repo(&pool).base.find_all().await.unwrap();
        assert_eq!(all.len(), 1);
        assert_eq!(all[0].chapter, "Ch. 1");
    }

    #[tokio::test]
    async fn scan_chapter_com_template_gera_sort_correto() {
        let (service, pool, dir) = setup().await;
        let file = create_file(&dir, "Ch. 10.cbz").await;
        service
            .scan_chapter(&file, 0, 1, None, Some("Ch. {chapter}{decimal}.*.{extension}"))
            .await
            .unwrap();
        let all = chapter_repo(&pool).base.find_all().await.unwrap();
        assert_eq!(all[0].chapter_sort, "10");
    }

    #[tokio::test]
    async fn scan_chapter_duplicado_e_ignorado() {
        let (service, pool, dir) = setup().await;
        let file = create_file(&dir, "Ch. 1.cbz").await;
        service.scan_chapter(&file, 0, 1, None, None).await.unwrap();
        service.scan_chapter(&file, 0, 1, None, None).await.unwrap();
        assert_eq!(chapter_repo(&pool).base.count().await.unwrap(), 1);
    }

    #[tokio::test]
    async fn scan_chapter_arquivo_inexistente_retorna_erro() {
        let (service, _, _) = setup().await;
        let fake = PathBuf::from("/nao/existe/Ch. 1.cbz");
        assert!(service.scan_chapter(&fake, 0, 1, None, None).await.is_err());
    }
}
