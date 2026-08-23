use serde::{Deserialize, Serialize};

use crate::callbacks::{FfiChapterReadEntry, FfiReadingProgressEntry};

/// Manifesto trocado uma única vez por sessão do protocolo `acerola/sync-history/1` —
/// contém o estado local inteiro de progresso de leitura e capítulos lidos, já que ambos
/// são pequenos (sem payload binário), permitindo que cada lado aplique sua própria lógica
/// de diff/conflito localmente, sem uma segunda rodada de rede.
///
/// Schema de wire compartilhado com o Desktop (`acerola-desktop/.../infra/sync/messages.rs`,
/// via `#[serde(rename)]` nos campos `entries`/`read_markers`/`chapter`). Os dois lados não
/// compartilham código, então os nomes de campo aqui (`reading_progress`, `chapters_read`,
/// `chapter_sort`) são o contrato — não renomear sem atualizar o outro lado também.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub(crate) struct HistoryManifest {
    pub reading_progress: Vec<FfiReadingProgressEntry>,
    pub chapters_read: Vec<FfiChapterReadEntry>,
}

#[derive(Debug, Clone, Default)]
pub(crate) struct HistorySyncStats {
    pub progress_applied: u32,
    pub progress_skipped: u32,
    pub chapters_read_applied: u32,
    pub chapters_read_skipped: u32,
}
