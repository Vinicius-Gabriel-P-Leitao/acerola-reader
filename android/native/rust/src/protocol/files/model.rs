use serde::{Deserialize, Serialize};

use crate::callbacks::FfiFileManifestEntry;

/// Todos os tipos de mensagem trocados ao longo de uma sessão do protocolo
/// `acerola/sync-files/1`. Schema compartilhado com o Desktop
/// (`acerola-desktop/.../infra/sync/messages.rs`) — os nomes e formatos aqui precisam
/// bater exatamente com os de lá, já que os dois lados não compartilham código.
///
/// Cada mensagem é um frame JSON solto (sem tag de enum) via length-delimited codec.
/// Os bytes de um capítulo não viajam dentro de uma mensagem JSON — são frames binários
/// crus enviados logo após o `FileHeader` correspondente, até somar `size` bytes.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub(crate) struct FileChapterInfo {
    pub chapter: String,
    pub file_name: String,
    pub checksum: Option<String>,
    pub size: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub(crate) struct FileComicInfo {
    pub comic_name: String,
    pub chapters: Vec<FileChapterInfo>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub(crate) struct FileManifest {
    pub comics: Vec<FileComicInfo>,
}

/// O que este lado quer *do outro lado* — pares `(comic_name, chapter)`.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub(crate) struct FileWantList {
    pub wanted: Vec<(String, String)>,
}

/// Fase 0 exclusiva do protocolo `acerola/sync-comic/1` (sincronização de um único
/// quadrinho) — trocada antes de qualquer coisa do protocolo `sync-files` normal. Só o lado
/// outbound sabe qual quadrinho o usuário escolheu (veio de uma chamada FFI Kotlin ->
/// `P2PNode::sync_comic`, não do wire); o inbound aprende por aqui e filtra seu próprio
/// manifesto ao mesmo `comic_name` antes de seguir para as fases 1-3 compartilhadas com
/// `run_exchange`.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub(crate) struct ComicSyncScope {
    pub comic_name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub(crate) struct FileHeader {
    pub comic_name: String,
    pub chapter: String,
    pub file_name: String,
    pub size: u64,
    /// SHA-256 do manifesto (`FileChapterInfo.checksum`) — repassado tal e qual pra
    /// `FileSyncProvider::begin_chapter_write`/verificação Kotlin, sem relação com blobs.
    pub checksum: Option<String>,
    /// Hash de blob (BLAKE3, hex) devolvido por `P2pBlobStore::put` no lado que enviou —
    /// é o que o lado que recebe usa em `P2pBlobStore::fetch` pra puxar os bytes de verdade.
    /// `None` só no header vazio de "não tenho mais esse capítulo" (`size: 0`).
    pub blob_hash: Option<String>,
}

/// Mensagem enviada no lugar do manifesto quando o `FileSyncSessionGuard` já rejeitou a
/// sessão localmente (peer já tem uma sessão ativa) — schema espelhado no Desktop
/// (`file_handler.rs`), único jeito dos dois lados falarem a mesma coisa mesmo sem
/// compartilhar código. O campo `error` é o discriminador: nenhuma outra mensagem do
/// protocolo usa essa chave (`FileManifest` usa `comics`, `FileWantList` usa `wanted`), então
/// quem lê pode detectar isso antes de tentar desserializar como `FileManifest`.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub(crate) struct SessionBusy {
    pub error: String,
    pub reason: String,
}

#[derive(Debug, Clone, Default)]
pub(crate) struct FileSyncStats {
    pub received_count: u32,
    pub sent_count: u32,
    pub failed_count: u32,
}

/// Agrupa a lista plana de `get_file_manifest()` (FFI) em `FileManifest` aninhado por
/// quadrinho — formato de wire compartilhado com o Desktop.
pub(super) fn build_manifest(entries: Vec<FfiFileManifestEntry>) -> FileManifest {
    use std::collections::HashMap;

    let mut by_comic: HashMap<String, Vec<FileChapterInfo>> = HashMap::new();
    for entry in entries {
        by_comic.entry(entry.comic_name).or_default().push(FileChapterInfo {
            chapter: entry.chapter,
            file_name: entry.file_name,
            checksum: Some(entry.checksum),
            size: entry.size_bytes,
        });
    }

    let comics =
        by_comic.into_iter().map(|(comic_name, chapters)| FileComicInfo { comic_name, chapters }).collect();

    FileManifest { comics }
}

#[cfg(test)]
mod wire_contract_tests {
    use super::*;

    /// Trava o schema de wire do `sync-files` contra o que o Desktop produz/espera
    /// (`acerola-desktop/.../infra/sync/messages.rs`) — os dois lados não compartilham
    /// código, então isso é o que garante que não divergem de novo silenciosamente.
    #[test]
    fn file_manifest_matches_desktop_wire_shape() {
        let manifest = build_manifest(vec![FfiFileManifestEntry {
            comic_name: "Berserk".into(),
            chapter: "Cap 1".into(),
            file_name: "Cap 1.cbz".into(),
            checksum: "abc123".into(),
            size_bytes: 42,
        }]);

        let value = serde_json::to_value(&manifest).unwrap();
        assert_eq!(
            value,
            serde_json::json!({
                "comics": [{
                    "comic_name": "Berserk",
                    "chapters": [{
                        "chapter": "Cap 1",
                        "file_name": "Cap 1.cbz",
                        "checksum": "abc123",
                        "size": 42
                    }]
                }]
            })
        );

        // Payload no exato formato que o Desktop envia (chave "comics", sem tag de enum)
        // precisa desserializar sem erro.
        let desktop_wire = serde_json::json!({
            "comics": [{
                "comic_name": "Berserk",
                "chapters": [{
                    "chapter": "Cap 1",
                    "file_name": "Cap 1.cbz",
                    "checksum": "abc123",
                    "size": 42
                }]
            }]
        });
        let decoded: FileManifest = serde_json::from_value(desktop_wire).unwrap();
        assert_eq!(decoded.comics[0].chapters[0].file_name, "Cap 1.cbz");
    }

    #[test]
    fn file_want_list_serializes_as_tuple_array() {
        let wanted = FileWantList { wanted: vec![("Berserk".into(), "Cap 1".into())] };
        let value = serde_json::to_value(&wanted).unwrap();
        assert_eq!(value, serde_json::json!({ "wanted": [["Berserk", "Cap 1"]] }));
    }
}
