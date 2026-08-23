use serde::{Deserialize, Serialize};

/// Único frame escrito pelo lado outbound — a conexão em si não é o pedido (diferente de
/// `browse-library`), porque aqui o outbound precisa informar QUAL quadrinho e qual versão já
/// tem cacheada, pra o inbound decidir entre `not_modified` e publicar um blob novo.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub(crate) struct CoverRequest {
    pub comic_name: String,
    /// `None` = "nunca busquei essa capa antes" — qualquer versão que o inbound tiver conta
    /// como "mudou".
    pub known_version: Option<i64>,
}

/// Discriminador de `CoverResponse.status` — sem tag de enum no wire (mesma convenção do resto
/// do protocolo), só uma `String` comparada por valor.
pub(crate) const STATUS_NOT_MODIFIED: &str = "not_modified";
pub(crate) const STATUS_CHANGED: &str = "changed";
pub(crate) const STATUS_UNAVAILABLE: &str = "unavailable";

#[derive(Debug, Clone, Serialize, Deserialize)]
pub(crate) struct CoverResponse {
    pub status: String,
    pub cover_version: Option<i64>,
    /// Só presente quando `status == "changed"` — hash de blob (BLAKE3, hex) que o outbound usa
    /// em `ChapterTransfer::fetch_reader` pra puxar os bytes de verdade.
    pub cover_hash: Option<String>,
}
