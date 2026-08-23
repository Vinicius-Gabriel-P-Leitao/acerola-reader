//! Traits estrangeiras (implementadas em Kotlin) usadas pelos protocolos de sync para ler/gravar
//! dados no Room/SAF do app, mantendo a lógica de manifesto/diff/aplicação inteiramente em Rust.

use serde::{Deserialize, Serialize};

/// Fonte/destino dos dados de histórico de leitura (`reading_history` + `chapter_read`) usada
/// pelo protocolo `acerola/sync-history/1`. Implementada em Kotlin contra o Room.
#[uniffi::export(with_foreign)]
pub trait HistorySyncProvider: Send + Sync {
    /// Todas as linhas locais de progresso de leitura, com chave natural (nome do quadrinho).
    fn get_reading_progress(&self) -> Vec<FfiReadingProgressEntry>;

    /// Todas as linhas locais de capítulos marcados como lidos, com chave natural.
    fn get_chapters_read(&self) -> Vec<FfiChapterReadEntry>;

    /// Aplica uma linha de progresso recebida do peer (Rust já decidiu que ela vence via LWW).
    /// Retorna `false` se o quadrinho referenciado não existir localmente (entrada ignorada).
    fn apply_reading_progress(&self, entry: FfiReadingProgressEntry) -> bool;

    /// Aplica uma linha de "capítulo lido" recebida do peer (união idempotente).
    /// Retorna `false` se o quadrinho referenciado não existir localmente (entrada ignorada).
    fn apply_chapter_read(&self, entry: FfiChapterReadEntry) -> bool;
}

#[derive(uniffi::Record, Debug, Clone, Serialize, Deserialize)]
pub struct FfiReadingProgressEntry {
    pub comic_name: String,
    pub chapter_sort: String,
    pub last_page: i32,
    pub is_completed: bool,
    pub updated_at: i64,
}

#[derive(uniffi::Record, Debug, Clone, Serialize, Deserialize)]
pub struct FfiChapterReadEntry {
    pub comic_name: String,
    pub chapter_sort: String,
    pub created_at: i64,
}

/// Fonte/destino dos arquivos de capítulo usada pelo protocolo `acerola/sync-files/1`.
/// Implementada em Kotlin contra SAF/DocumentFile. Baseada em handles opacos (`i64`) porque
/// streams não cruzam a fronteira FFI diretamente — o Kotlin mantém um mapa de handles abertos.
#[uniffi::export(with_foreign)]
pub trait FileSyncProvider: Send + Sync {
    /// Todos os capítulos locais com checksum/tamanho, com chave natural `(comic_name, chapter)`.
    fn get_file_manifest(&self) -> Vec<FfiFileManifestEntry>;

    /// Abre um capítulo local para leitura em chunks (lado que envia). `-1` se não encontrado.
    fn open_chapter_for_read(&self, comic_name: String, chapter: String) -> i64;

    /// Lê até `chunk_size` bytes do handle aberto; um `Vec` vazio sinaliza fim de arquivo.
    fn read_chapter_chunk(&self, handle: i64, chunk_size: u32) -> Vec<u8>;

    /// Fecha um handle de leitura aberto por `open_chapter_for_read`.
    fn close_read_handle(&self, handle: i64);

    /// Começa a receber um capítulo (lado que recebe): cria um arquivo temporário em `synced/`.
    /// `file_name` é o nome de arquivo real anunciado pelo peer no header (schema compartilhado
    /// com o Desktop) — usado como nome final em vez do rótulo do capítulo, pra preservar a
    /// extensão original (.cbz/.cbr) do lado que enviou.
    fn begin_chapter_write(
        &self,
        comic_name: String,
        chapter: String,
        file_name: String,
        expected_checksum: String,
        size_bytes: u64,
    ) -> i64;

    /// Grava um chunk recebido no handle de escrita aberto por `begin_chapter_write`.
    fn write_chapter_chunk(&self, handle: i64, bytes: Vec<u8>) -> bool;

    /// Verifica o checksum do arquivo temporário e o move pro destino final em `synced/`.
    fn finalize_chapter_write(&self, handle: i64) -> bool;

    /// Aborta uma escrita em andamento (peer desconectou no meio, checksum não bateu, etc).
    fn abort_chapter_write(&self, handle: i64);
}

/// Armazenamento criptografado (Android Keystore, via `EncryptedSharedPreferences`) usado por
/// `SecureP2pStorage`/`SecureTrustedStore` (ver `storage.rs`/`trust_store.rs`) pra persistir
/// identidade do nó, cache de peers e lista de confiança TOFU sem tocar texto puro em disco.
/// Chave/valor puros — todo o layout (quais chaves existem, formato JSON de cada uma) é
/// decidido do lado Rust, o Kotlin só faz o armazenamento opaco.
#[uniffi::export(with_foreign)]
pub trait SecureBlobStore: Send + Sync {
    /// `Err` só em falha real de backend (ex.: `EncryptedSharedPreferences`/Keystore
    /// indisponível ou corrompido) — nunca usado pra "chave não existe ainda", que não é
    /// um erro. Distinguir os dois importa: se `load_identity` tratasse falha real como
    /// "identidade nunca existiu", o node mintaria uma identidade nova silenciosamente e
    /// quebraria todo o pareamento já feito sem avisar ninguém.
    fn save_blob(&self, key: String, value: Vec<u8>) -> Result<(), SecureBlobStoreError>;

    /// `Ok(None)` se a chave nunca foi salva (estado normal). `Err` só em falha real de
    /// backend — ver `save_blob`.
    fn load_blob(&self, key: String) -> Result<Option<Vec<u8>>, SecureBlobStoreError>;
}

#[derive(uniffi::Error, thiserror::Error, Debug)]
pub enum SecureBlobStoreError {
    #[error("secure blob store access failed: {reason}")]
    AccessFailed { reason: String },
}

#[derive(uniffi::Record, Debug, Clone, Serialize, Deserialize)]
pub struct FfiFileManifestEntry {
    pub comic_name: String,
    pub chapter: String,
    /// Nome de arquivo real (com extensão), lido do `DocumentFile` no Kotlin — necessário
    /// pra falar o mesmo schema de wire que o Desktop usa em `FileHeader`/`FileChapterInfo`.
    pub file_name: String,
    pub checksum: String,
    pub size_bytes: u64,
}
