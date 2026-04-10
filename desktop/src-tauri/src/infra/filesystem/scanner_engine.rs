use std::future::Future;
use std::path::PathBuf;
use std::pin::Pin;
use tokio::fs;
use tokio::sync::mpsc;

/// Um diretório e os arquivos encontrados diretamente dentro dele.
pub struct DirectoryEntry {
    pub directory: PathBuf,
    pub files: Vec<PathBuf>,
}

pub struct ScannerEngine {
    pub max_depth: Option<usize>,
}

// FIXME: Colocar tratamento de erros
impl ScannerEngine {
    pub fn new() -> Self {
        Self { max_depth: None }
    }

    /// Escaneia `root` recursivamente e emite via channel um [`DirectoryEntry`]
    /// por pasta que contiver arquivos — sem acumular tudo na heap.
    pub async fn scan(
        &self,
        root: PathBuf,
        tx: mpsc::Sender<DirectoryEntry>
    ) -> Result<(), std::io::Error> {
        self.walk(&root, &tx, 0).await
    }

    fn walk<'a>(
        &'a self,
        path: &'a PathBuf,
        tx: &'a mpsc::Sender<DirectoryEntry>,
        depth: usize
    ) -> Pin<Box<dyn Future<Output = Result<(), std::io::Error>> + Send + 'a>> {
        Box::pin(async move {
            if let Some(max) = self.max_depth {
                if depth > max {
                    return Ok(());
                }
            }

            let mut entries = fs::read_dir(path).await?;
            let mut subdirs: Vec<PathBuf> = vec![];
            let mut files: Vec<PathBuf> = vec![];

            while let Ok(Some(entry)) = entries.next_entry().await {
                let entry_path = entry.path();
                let meta = entry.metadata().await?;

                if meta.is_dir() {
                    subdirs.push(entry_path);
                    continue;
                }

                if meta.is_file() {
                    files.push(entry_path);
                }
            }

            // Emite esse diretório se tiver arquivos — sem guardar tudo na heap
            if !files.is_empty() {
                let _ = tx.send(DirectoryEntry {
                    directory: path.clone(),
                    files,
                }).await;
            }

            // Desce nos subdiretórios depois de emitir — libera a heap do atual
            for subdir in subdirs {
                self.walk(&subdir, tx, depth + 1).await?;
            }

            Ok(())
        })
    }
}

#[cfg(test)]
mod tests {
    use super::{ DirectoryEntry, ScannerEngine };
    use std::fs;
    use tempfile::tempdir;
    use tokio::sync::mpsc;

    async fn collect(root: std::path::PathBuf) -> Vec<DirectoryEntry> {
        let (tx, mut rx) = mpsc::channel(32);
        let engine = ScannerEngine::new();

        tokio::spawn(async move {
            engine.scan(root, tx).await.unwrap();
        });

        let mut result = vec![];
        while let Some(entry) = rx.recv().await {
            result.push(entry);
        }
        result
    }

    #[tokio::test]
    async fn teste_encontra_arquivos_no_root() {
        let root = tempdir().unwrap();

        fs::write(root.path().join("cap1.cbz"), b"").unwrap();
        fs::write(root.path().join("cap2.cbz"), b"").unwrap();

        let entries = collect(root.path().to_path_buf()).await;

        assert_eq!(entries.len(), 1);
        assert_eq!(entries[0].files.len(), 2);
    }

    #[tokio::test]
    async fn teste_associa_arquivos_ao_diretorio_pai() {
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

        assert_eq!(entries.len(), 2);
        assert_eq!(entries[0].directory, berserk);
        assert_eq!(entries[0].files.len(), 2);
        assert_eq!(entries[1].directory, hq);
        assert_eq!(entries[1].files.len(), 1);
    }

    #[tokio::test]
    async fn teste_escaneia_em_profundidade() {
        let root = tempdir().unwrap();

        let mangas = root.path().join("Mangas").join("Berserk");
        fs::create_dir_all(&mangas).unwrap();
        fs::write(mangas.join("cap1.cbz"), b"").unwrap();

        let entries = collect(root.path().to_path_buf()).await;

        assert_eq!(entries.len(), 1);
        assert_eq!(entries[0].directory, mangas);
    }

    #[tokio::test]
    async fn teste_respeita_max_depth() {
        let root = tempdir().unwrap();

        let level1 = root.path().join("Mangas");
        let level2 = level1.join("Berserk");
        fs::create_dir_all(&level2).unwrap();

        fs::write(level1.join("cap1.cbz"), b"").unwrap(); // depth 1 — entra
        fs::write(level2.join("cap2.cbz"), b"").unwrap(); // depth 2 — bloqueado

        let (tx, mut rx) = mpsc::channel(32);
        let engine = ScannerEngine { max_depth: Some(1) };
        let root_path = root.path().to_path_buf();

        tokio::spawn(async move {
            engine.scan(root_path, tx).await.unwrap();
        });

        let mut entries = vec![];
        while let Some(entry) = rx.recv().await {
            entries.push(entry);
        }

        assert_eq!(entries.len(), 1);
        assert_eq!(entries[0].directory, level1);
    }

    #[tokio::test]
    async fn teste_diretorio_vazio_nao_emite_entrada() {
        let root = tempdir().unwrap();
        fs::create_dir_all(root.path().join("vazio")).unwrap();

        let entries = collect(root.path().to_path_buf()).await;

        assert_eq!(entries.len(), 0);
    }
}
