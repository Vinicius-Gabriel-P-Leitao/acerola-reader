use std::path::{ Path, PathBuf };

pub struct PathGuard {
    allowed_root: PathBuf,
}

impl PathGuard {
    pub fn new(root: PathBuf) -> Self {
        Self { allowed_root: root.canonicalize().unwrap_or(root) }
    }

    fn is_valid(&self, path: &Path) -> bool {
        match path.canonicalize() {
            Ok(p) => p.starts_with(&self.allowed_root),
            Err(_) => false,
        }
    }

    pub fn execute<F, R, E>(&self, path: &Path, action: F) -> Result<R, String>
        where F: FnOnce(&Path) -> Result<R, E>, E: std::fmt::Display
    {
        if !self.is_valid(path) {
            return Err("Access denied: path outside the allowed directory.".into());
        }

        action(path).map_err(|error: E| error.to_string())
    }
}

#[cfg(test)]
mod tests {
    use tempfile::tempdir;
    use super::PathGuard;
    use std::fs;

    #[test]
    fn teste_caminho_valido_dentro_do_root() {
        let root = tempdir().unwrap();
        let guard = PathGuard::new(root.path().to_path_buf());

        let file = root.path().join("arquivo.cbz");
        fs::write(&file, b"").unwrap();

        let result = guard.execute(&file, |_| -> Result<(), String> { Ok(()) });

        assert!(result.is_ok());
    }

    #[test]
    fn teste_caminho_fora_do_root_e_negado() {
        let root = tempdir().unwrap();
        let outside = tempdir().unwrap(); // diretório diferente do root
        let guard = PathGuard::new(root.path().to_path_buf());

        let file = outside.path().join("arquivo.cbz");
        fs::write(&file, b"").unwrap();

        let result = guard.execute(&file, |_| -> Result<(), String> { Ok(()) });

        assert!(result.is_err());
        assert!(result.unwrap_err().contains("Access denied"));
    }

    #[test]
    fn teste_caminho_inexistente_e_negado() {
        let root = tempdir().unwrap();
        let guard = PathGuard::new(root.path().to_path_buf());

        let fake = root.path().join("nao_existe.cbz"); // não cria o arquivo

        let result = guard.execute(&fake, |_| -> Result<(), String> { Ok(()) });

        assert!(result.is_err());
    }

    #[test]
    fn teste_path_traversal_e_negado() {
        let root = tempdir().unwrap();
        let guard = PathGuard::new(root.path().to_path_buf());

        // tentativa de sair do root via ../
        let traversal = root.path().join("../arquivo_malicioso.cbz");

        let result = guard.execute(&traversal, |_| -> Result<(), String> { Ok(()) });

        assert!(result.is_err());
    }
}
