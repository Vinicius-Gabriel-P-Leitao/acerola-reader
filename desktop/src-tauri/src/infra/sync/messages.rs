use serde::{Deserialize, Serialize};

/// Uma entrada de progresso de leitura, identificada pela chave natural
/// (nome do quadrinho + rótulo do capítulo) — a única forma comparável entre
/// duas bases SQLite independentes, já que os IDs autoincrement não são portáveis.
///
/// Nomes de campo no wire (`chapter_sort` em vez de `chapter`) seguem o schema já
/// usado pelo Android em `protocol/history/model.rs` — schema compartilhado do ALPN
/// `acerola/sync-history/1`, os nomes Rust internos não mudam.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HistoryEntry {
    pub comic_name: String,
    #[serde(rename = "chapter_sort")]
    pub chapter: String,
    pub last_page: i64,
    pub is_completed: bool,
    pub updated_at: i64,
}

/// Um marcador de "capítulo lido". Não tem `updated_at` — é um fato binário que só
/// existe ou não, então o merge entre devices é uma união (insert_or_ignore), não
/// last-write-wins.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ReadMarker {
    pub comic_name: String,
    #[serde(rename = "chapter_sort")]
    pub chapter: String,
    pub created_at: i64,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct HistoryManifest {
    #[serde(rename = "reading_progress")]
    pub entries: Vec<HistoryEntry>,
    #[serde(rename = "chapters_read")]
    pub read_markers: Vec<ReadMarker>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileChapterInfo {
    pub chapter: String,
    pub file_name: String,
    pub checksum: Option<String>,
    pub size: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileComicInfo {
    pub comic_name: String,
    pub chapters: Vec<FileChapterInfo>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct FileManifest {
    pub comics: Vec<FileComicInfo>,
}

/// O que este lado quer *do outro lado* — calculado localmente depois de comparar
/// os dois manifestos, e enviado de volta pro peer decidir o que transmitir.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct FileWantList {
    pub wanted: Vec<(String, String)>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileHeader {
    pub comic_name: String,
    pub chapter: String,
    pub file_name: String,
    pub size: u64,
    pub checksum: Option<String>,
}

/// Mensagem enviada no lugar do manifesto quando `FileSyncSessionGuard` já rejeitou a sessão
/// localmente (peer já tem uma sessão ativa) — schema espelhado no Android
/// (`protocol/files/model.rs::SessionBusy`), único jeito dos dois lados falarem a mesma coisa
/// mesmo sem compartilhar código. O campo `error` é o discriminador: nenhuma outra mensagem
/// do protocolo de arquivos usa essa chave (`FileManifest` usa `comics`, `FileWantList` usa
/// `wanted`), então quem lê pode detectar isso antes de tentar desserializar como
/// `FileManifest`.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SessionBusy {
    pub error: String,
    pub reason: String,
}

#[cfg(test)]
mod wire_contract_tests {
    use super::*;

    /// Trava o schema de wire do `HistoryManifest` contra o formato que o Android
    /// (`protocol/history/model.rs` + `callbacks.rs`) produz e espera — os dois lados
    /// não compartilham código, então esse teste é o que garante que não divergem de
    /// novo silenciosamente.
    #[test]
    fn history_manifest_matches_android_wire_shape() {
        let manifest = HistoryManifest {
            entries: vec![HistoryEntry {
                comic_name: "Berserk".into(),
                chapter: "12".into(),
                last_page: 5,
                is_completed: false,
                updated_at: 1000,
            }],
            read_markers: vec![ReadMarker {
                comic_name: "Berserk".into(),
                chapter: "11".into(),
                created_at: 900,
            }],
        };

        let value = serde_json::to_value(&manifest).unwrap();
        assert_eq!(
            value,
            serde_json::json!({
                "reading_progress": [{
                    "comic_name": "Berserk",
                    "chapter_sort": "12",
                    "last_page": 5,
                    "is_completed": false,
                    "updated_at": 1000
                }],
                "chapters_read": [{
                    "comic_name": "Berserk",
                    "chapter_sort": "11",
                    "created_at": 900
                }]
            })
        );

        // Mensagem exatamente no formato que o Android envia (chaves reading_progress/
        // chapters_read, campo chapter_sort) precisa desserializar sem erro.
        let android_wire = serde_json::json!({
            "reading_progress": [{
                "comic_name": "Berserk",
                "chapter_sort": "12",
                "last_page": 5,
                "is_completed": false,
                "updated_at": 1000
            }],
            "chapters_read": []
        });
        let decoded: HistoryManifest = serde_json::from_value(android_wire).unwrap();
        assert_eq!(decoded.entries[0].chapter, "12");
    }
}
