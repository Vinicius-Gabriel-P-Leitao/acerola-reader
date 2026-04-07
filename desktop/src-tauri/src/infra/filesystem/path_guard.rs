use std::path::{ Path, PathBuf };
pub struct PathGuard {
    allowed_root: PathBuf,
}

impl PathGuard {
    pub fn new(root: PathBuf) -> Self {
        Self { allowed_root: root }
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
            return Err("Acesso negado: Path fora da biblioteca permitida.".into());
        }

        action(path).map_err(|error: E| error.to_string())
    }
}
