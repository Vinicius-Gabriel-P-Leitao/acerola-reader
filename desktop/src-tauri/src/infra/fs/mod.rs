use std::{future::Future, path::PathBuf, pin::Pin, sync::Arc};

use tokio::{fs, sync::mpsc};

/// Representa uma entrada de diretório descoberta durante o escaneamento.
///
/// Contém o caminho do diretório, a lista de arquivos filtrados e a lista de
/// subdiretórios imediatos.
pub struct DirectoryEntry {
    pub directory: PathBuf,
    pub files: Vec<PathBuf>,
    pub subdirs: Vec<PathBuf>,
}

/// Motor de busca recursivo para arquivos no sistema de arquivos.
///
/// O `ScannerEngine` percorre árvores de diretórios de forma assíncrona, enviando
/// notificações de pastas encontradas através de um canal (`mpsc`).
pub struct ScannerEngine {
    /// Profundidade máxima de recursão permitida. `None` indica profundidade ilimitada.
    pub max_depth: Option<usize>,
}

impl ScannerEngine {
    pub fn new() -> Self {
        Self { max_depth: None }
    }

    /// Inicia o escaneamento recursivo a partir de `root`.
    ///
    /// # Argumentos
    /// * `root` - O ponto de partida no sistema de arquivos.
    /// * `tx` - Canal para envio dos resultados encontrados.
    /// * `filter` - Função de filtragem para decidir quais arquivos devem ser incluídos no scan.
    pub async fn scan<F>(
        &self, root: PathBuf, tx: mpsc::Sender<DirectoryEntry>, filter: F,
    ) -> Result<(), std::io::Error>
    where
        F: Fn(&PathBuf) -> bool + Send + Sync + 'static,
    {
        let shared_filter = Arc::new(filter);
        self.walk(root, tx, 0, shared_filter).await.map(|_| ())
    }

    /// Função recursiva interna para caminhar pela árvore de diretórios.
    fn walk<F>(
        &self, path: PathBuf, tx: mpsc::Sender<DirectoryEntry>, depth: usize, filter: Arc<F>,
    ) -> Pin<Box<dyn Future<Output = Result<bool, std::io::Error>> + Send + '_>>
    where
        F: Fn(&PathBuf) -> bool + Send + Sync + 'static,
    {
        Box::pin(async move {
            if let Some(max) = self.max_depth {
                if depth > max {
                    return Ok(false);
                }
            }

            let mut entries = match fs::read_dir(&path).await {
                Ok(e) => e,
                Err(err) => {
                    tracing::warn!("Scanner error reading directory {:?}: {}", path, err);
                    return Ok(false);
                },
            };
            let mut subdirs: Vec<PathBuf> = vec![];
            let mut files: Vec<PathBuf> = vec![];

            while let Ok(Some(entry)) = entries.next_entry().await {
                let entry_path = entry.path();
                let metadata = match entry.metadata().await {
                    Ok(m) => m,
                    Err(_) => continue,
                };

                if metadata.is_dir() {
                    subdirs.push(entry_path);
                    continue;
                }

                if metadata.is_file() && filter(&entry_path) {
                    files.push(entry_path);
                }
            }

            let mut child_found = false;
            for subdirectory in subdirs.clone() {
                match self.walk(subdirectory, tx.clone(), depth + 1, filter.clone()).await {
                    Ok(true) => child_found = true,
                    Ok(false) => {},
                    Err(err) => {
                        tracing::warn!("Scanner error during walk: {}", err);
                    },
                }
            }

            let should_emit = !files.is_empty() || child_found;

            if should_emit {
                let _ = tx.send(DirectoryEntry { directory: path, files, subdirs }).await;
            }

            Ok(should_emit)
        })
    }
}

#[cfg(test)]
mod tests {
    use std::fs;

    use tempfile::tempdir;
    use tokio::sync::mpsc;

    use super::{DirectoryEntry, ScannerEngine};

    async fn collect(root: std::path::PathBuf) -> Vec<DirectoryEntry> {
        let (tx, mut rx) = mpsc::channel(32);
        let engine = ScannerEngine::new();

        tokio::spawn(async move {
            engine.scan(root, tx, |_| true).await.unwrap();
        });

        let mut result = vec![];
        while let Some(entry) = rx.recv().await {
            result.push(entry);
        }
        result
    }

    #[tokio::test]
    async fn test_finds_files_in_root() {
        let root = tempdir().unwrap();

        fs::write(root.path().join("cap1.cbz"), b"").unwrap();
        fs::write(root.path().join("cap2.cbz"), b"").unwrap();

        let entries = collect(root.path().to_path_buf()).await;

        assert_eq!(entries.len(), 1);
        assert_eq!(entries[0].files.len(), 2);
    }

    #[tokio::test]
    async fn test_associates_files_with_parent_directory() {
        let root = tempdir().unwrap();

        let berserk = root.path().join("Berserk");
        let hq = root.path().join("HQ");
        fs::create_dir_all(&berserk).unwrap();
        fs::create_dir_all(&hq).unwrap();

        fs::write(berserk.join("cap1.cbz"), b"").unwrap();
        fs::write(berserk.join("cap2.cbz"), b"").unwrap();
        fs::write(hq.join("hq1.cbz"), b"").unwrap();

        let mut entries = collect(root.path().to_path_buf()).await;
        entries.sort_by_key(|e| e.directory.clone());

        assert_eq!(entries.len(), 3);

        let paths: Vec<_> = entries.iter().map(|e| e.directory.clone()).collect();
        assert!(paths.contains(&root.path().to_path_buf()));
        assert!(paths.contains(&berserk));
        assert!(paths.contains(&hq));
    }

    #[tokio::test]
    async fn test_scans_in_depth() {
        let root = tempdir().unwrap();

        let mangas = root.path().join("Mangas").join("Berserk");
        fs::create_dir_all(&mangas).unwrap();
        fs::write(mangas.join("cap1.cbz"), b"").unwrap();

        let entries = collect(root.path().to_path_buf()).await;

        assert_eq!(entries.len(), 3);
        let paths: Vec<_> = entries.iter().map(|e| e.directory.clone()).collect();
        assert!(paths.contains(&mangas));
    }

    #[tokio::test]
    async fn test_respects_max_depth() {
        let root = tempdir().unwrap();

        let level1 = root.path().join("Mangas");
        let level2 = level1.join("Berserk");
        fs::create_dir_all(&level2).unwrap();

        fs::write(level1.join("cap1.cbz"), b"").unwrap();
        fs::write(level2.join("cap2.cbz"), b"").unwrap();

        let (tx, mut rx) = mpsc::channel(32);
        let engine = ScannerEngine { max_depth: Some(1) };
        let root_path = root.path().to_path_buf();

        tokio::spawn(async move {
            engine.scan(root_path, tx, |_| true).await.unwrap();
        });

        let mut entries = vec![];
        while let Some(entry) = rx.recv().await {
            entries.push(entry);
        }

        assert_eq!(entries.len(), 2);
    }

    #[tokio::test]
    async fn test_empty_directory_does_not_emit_entry() {
        let root = tempdir().unwrap();
        fs::create_dir_all(root.path().join("vazio")).unwrap();

        let entries = collect(root.path().to_path_buf()).await;

        assert_eq!(entries.len(), 0);
    }
}
