use super::ConnectionError;

/// `iroh_blobs::api::Error` já centraliza os erros de RPC/IO internos do `iroh-blobs`
/// (conexões RPC quebradas, falhas de I/O em disco) num único wrapper — não precisamos de um
/// `match` por variante como em `iroh.rs`, só reportar e mapear pro caso genérico de stream.
impl From<iroh_blobs::api::Error> for ConnectionError {
    fn from(err: iroh_blobs::api::Error) -> Self {
        tracing::debug!(layer = "iroh_blobs", error = ?err, "blob store operation failed");
        ConnectionError::StreamFailed(err.to_string())
    }
}

impl From<iroh_blobs::api::RequestError> for ConnectionError {
    fn from(err: iroh_blobs::api::RequestError) -> Self {
        ConnectionError::from(iroh_blobs::api::Error::from(err))
    }
}

/// Erro de uma requisição `get` (usado por `Remote::fetch`) — tipo próprio do `iroh-blobs`,
/// não convertível para `api::Error` porque cobre falhas específicas do protocolo de streaming
/// (decodificação, handshake de blob) além de I/O puro.
impl From<iroh_blobs::get::GetError> for ConnectionError {
    fn from(err: iroh_blobs::get::GetError) -> Self {
        tracing::debug!(layer = "iroh_blobs", error = ?err, "blob fetch from peer failed");
        ConnectionError::StreamFailed(err.to_string())
    }
}

/// Erro de baixo nível do transporte RPC interno do `iroh-blobs` (`store.tags()`/`store.blobs()`
/// se comunicam com o ator do store via `irpc`).
impl From<irpc::Error> for ConnectionError {
    fn from(err: irpc::Error) -> Self {
        tracing::debug!(layer = "iroh_blobs", error = ?err, "blob store rpc failed");
        ConnectionError::StreamFailed(err.to_string())
    }
}

/// Erro genérico do `n0-error` (framework de erros do ecossistema n0/iroh) — usado por
/// `FsStore::load_with_opts` ao abrir/criar o store em disco. Falha aqui é sempre um problema de
/// inicialização (ex: diretório sem permissão), nunca de uma stream já em andamento.
impl From<n0_error::AnyError> for ConnectionError {
    fn from(err: n0_error::AnyError) -> Self {
        tracing::debug!(layer = "iroh_blobs", error = ?err, "blob store initialization failed");
        ConnectionError::StartupFailed(err.to_string())
    }
}
