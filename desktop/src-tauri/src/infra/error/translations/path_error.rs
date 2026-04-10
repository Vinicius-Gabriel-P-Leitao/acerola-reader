use std::path::{ Path, PathBuf };
use thiserror::Error;

/// Erros que podem ocorrer durante a validação ou execução de operações com paths.
#[derive(Debug, Error)]
pub enum PathError {
    /// O caminho está fora do diretório raiz permitido.
    ///
    /// Indica uma tentativa de acesso fora dos limites definidos — seja por path
    /// traversal (`../`) ou por um path completamente diferente do root.
    #[error("Acesso negado: caminho fora do diretório permitido.")]
    AccessDenied,

    /// O caminho não existe ou não pôde ser resolvido pelo sistema operacional.
    ///
    /// Ocorre quando [`std::fs::canonicalize`] falha — o arquivo ou diretório
    /// não existe no momento da validação.
    #[error("Caminho não encontrado ou inacessível: {0}")]
    NotFound(PathBuf),

    /// A operação executada sobre o path falhou.
    ///
    /// Carrega a mensagem de erro da operação original.
    #[error("Falha ao executar operação no arquivo: {0}")]
    ActionFailed(String),
}

impl PathError {
    pub fn not_found(path: &Path) -> Self {
        log::debug!("[PathError] Caminho não encontrado: {:?}", path);
        PathError::NotFound(path.to_path_buf())
    }

    pub fn access_denied(canonical: &Path, root: &Path) -> Self {
        log::warn!("[PathError] Acesso negado: {:?} está fora de {:?}", canonical, root);
        PathError::AccessDenied
    }

    pub fn action_failed(path: &Path, msg: impl std::fmt::Display) -> Self {
        log::error!("[PathError] Falha na operação sobre {:?}: {}", path, msg);
        PathError::ActionFailed(msg.to_string())
    }
}
