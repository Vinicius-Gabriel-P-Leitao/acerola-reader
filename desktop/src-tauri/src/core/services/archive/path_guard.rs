use crate::infra::error::messages::path_error::PathError;
use std::collections::hash_map::DefaultHasher;
use std::hash::{Hash, Hasher};
use std::path::{Path, PathBuf};

pub fn path_hash(path: &Path) -> i64 {
    let mut hasher = DefaultHasher::new();
    path.hash(&mut hasher);
    (hasher.finish() & 0x7fff_ffff_ffff_ffff) as i64
}

pub struct PathGuard {
    allowed_root: PathBuf,
}

impl PathGuard {
    pub fn new(root: PathBuf) -> Self {
        Self { allowed_root: root.canonicalize().unwrap_or(root) }
    }

    fn validate(&self, path: &Path) -> Result<(), PathError> {
        let canonical = path.canonicalize().map_err(|_| PathError::not_found(path))?;

        if !canonical.starts_with(&self.allowed_root) {
            return Err(PathError::access_denied(&canonical, &self.allowed_root));
        }

        Ok(())
    }

    pub fn execute<F, R, E>(&self, path: &Path, action: F) -> Result<R, PathError>
    where
        F: FnOnce(&Path) -> Result<R, E>,
        E: std::fmt::Display,
    {
        self.validate(path)?;
        action(path).map_err(|err: E| PathError::action_failed(path, err))
    }
}

#[cfg(test)]
mod tests {
    use super::PathGuard;
    use crate::infra::error::messages::path_error::PathError;
    use std::fs;
    use tempfile::tempdir;

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
        let outside = tempdir().unwrap();
        let guard = PathGuard::new(root.path().to_path_buf());
        let file = outside.path().join("arquivo.cbz");
        fs::write(&file, b"").unwrap();
        let result = guard.execute(&file, |_| -> Result<(), String> { Ok(()) });
        assert!(matches!(result, Err(PathError::AccessDenied)));
    }

    #[test]
    fn teste_caminho_inexistente_e_negado() {
        let root = tempdir().unwrap();
        let guard = PathGuard::new(root.path().to_path_buf());
        let fake = root.path().join("nao_existe.cbz");
        let result = guard.execute(&fake, |_| -> Result<(), String> { Ok(()) });
        assert!(matches!(result, Err(PathError::NotFound(_))));
    }

    #[test]
    fn teste_path_traversal_e_negado() {
        let root = tempdir().unwrap();
        let guard = PathGuard::new(root.path().to_path_buf());
        let traversal = root.path().join("../arquivo_malicioso.cbz");
        let result = guard.execute(&traversal, |_| -> Result<(), String> { Ok(()) });
        assert!(matches!(result, Err(PathError::AccessDenied) | Err(PathError::NotFound(_))));
    }

    #[test]
    fn teste_action_failure_e_propagado() {
        let root = tempdir().unwrap();
        let guard = PathGuard::new(root.path().to_path_buf());
        let file = root.path().join("arquivo.cbz");
        fs::write(&file, b"").unwrap();
        let result =
            guard.execute(&file, |_| -> Result<(), String> { Err("falha simulada".to_string()) });
        assert!(matches!(result, Err(PathError::ActionFailed(_))));
    }
}
