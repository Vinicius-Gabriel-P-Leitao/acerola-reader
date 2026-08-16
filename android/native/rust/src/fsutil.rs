use std::{
    io,
    path::Path,
};

/// Escreve `bytes` em `path` de forma atômica: grava num arquivo temporário no mesmo
/// diretório e só então renomeia por cima do destino final, evitando arquivos truncados
/// se o processo morrer no meio da escrita.
pub(crate) fn atomic_write(path: &Path, bytes: &[u8]) -> io::Result<()> {
    let tmp_path = path.with_extension("tmp");
    std::fs::write(&tmp_path, bytes)?;
    std::fs::rename(&tmp_path, path)
}

/// Lê `path` inteiro, retornando `None` se o arquivo simplesmente não existe.
pub(crate) fn read_optional(path: &Path) -> io::Result<Option<Vec<u8>>> {
    match std::fs::read(path) {
        Ok(bytes) => Ok(Some(bytes)),
        Err(err) if err.kind() == io::ErrorKind::NotFound => Ok(None),
        Err(err) => Err(err),
    }
}
